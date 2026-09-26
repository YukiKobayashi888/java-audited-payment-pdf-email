package learning.fintech.report;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiEmailClient {
    private static final String EMAIL_SEND_PATH = "/v1/email/send";
    private final ReportConfiguration configuration;
    private final HttpClient http;

    public InfraiEmailClient(ReportConfiguration configuration) {
        this(configuration, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    InfraiEmailClient(ReportConfiguration configuration, HttpClient http) {
        this.configuration = configuration;
        this.http = http;
    }

    // Canonical call: POST /v1/email/send
    public SendReceipt send(String to, String subject, String html, String idempotencyKey)
            throws IOException, InterruptedException {
        String body = "{\"to\":" + quote(to) + ",\"subject\":" + quote(subject)
                + ",\"html\":" + quote(html) + "}";

        for (int attempt = 0; attempt < configuration.maxAttempts(); attempt++) {
            HttpRequest request = HttpRequest.newBuilder(configuration.apiBase().resolve(EMAIL_SEND_PATH))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + configuration.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.decode(response.body());

            if (response.statusCode() == 429 && attempt + 1 < configuration.maxAttempts()) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiException(envelope.errorCode(), envelope.errorMessage(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("email transport returned HTTP " + response.statusCode());
            }
            return new SendReceipt(envelope.messageId(), envelope.metadataJson());
        }
        throw new IOException("email request exhausted its retry policy");
    }

    private Duration retryDelay(HttpResponse<String> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try {
            long seconds = Long.parseLong(value);
            return Duration.ofSeconds(Math.max(0, seconds));
        } catch (NumberFormatException ignored) {
            return configuration.baseDelay().multipliedBy(1L << attempt);
        }
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c < 0x20 ? String.format("\\u%04x", (int) c) : c);
            }
        }
        return escaped.append('"').toString();
    }

    private record Envelope(boolean ok, String messageId, String errorCode, String errorMessage, String metadataJson) {
        private static Envelope decode(String json) throws IOException {
            boolean ok = booleanField(json, "ok");
            String data = objectField(json, "data");
            String error = objectField(json, "error");
            String metadata = objectField(json, "metadata");
            return new Envelope(ok, stringField(data, "message_id"), stringField(error, "code"),
                    stringField(error, "message"), metadata);
        }

        private static boolean booleanField(String json, String name) throws IOException {
            Matcher match = Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*(true|false)").matcher(json);
            if (!match.find()) throw new IOException("response envelope is missing " + name);
            return Boolean.parseBoolean(match.group(1));
        }

        private static String stringField(String json, String name) {
            if (json == null) return null;
            Matcher match = Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
            return match.find() ? match.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
        }

        private static String objectField(String json, String name) {
            Matcher start = Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*\\{").matcher(json);
            if (!start.find()) return null;
            int depth = 1;
            boolean quoted = false;
            boolean escaped = false;
            for (int i = start.end(); i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) { escaped = false; continue; }
                if (c == '\\' && quoted) { escaped = true; continue; }
                if (c == '"') { quoted = !quoted; continue; }
                if (!quoted && c == '{') depth++;
                if (!quoted && c == '}' && --depth == 0) return json.substring(start.end() - 1, i + 1);
            }
            return null;
        }
    }

    public record SendReceipt(String messageId, String metadataJson) { }

    public static final class InfraiException extends IOException {
        private final String code;
        private final int statusCode;

        public InfraiException(String code, String message, int statusCode) {
            super(message == null ? "email request was rejected" : message);
            this.code = code;
            this.statusCode = statusCode;
        }

        public String code() { return code; }
        public int statusCode() { return statusCode; }
    }
}

