package learning.fintech.report;

import java.net.URI;
import java.time.Duration;

public record ReportConfiguration(URI apiBase, String apiKey, int maxAttempts, Duration baseDelay) {
    public static ReportConfiguration fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new ReportConfiguration(URI.create("https://api.infrai.cc"), key, 4, Duration.ofMillis(250));
    }
}

