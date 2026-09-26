package learning.fintech.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

public final class PaymentReportService {
    private final ReportPolicy policy;
    private final AuditPdfReport pdfReport;
    private final InfraiEmailClient emailClient;

    public PaymentReportService(ReportPolicy policy, AuditPdfReport pdfReport, InfraiEmailClient emailClient) {
        this.policy = policy;
        this.pdfReport = pdfReport;
        this.emailClient = emailClient;
    }

    public Outcome handle(PaymentEvent event) throws IOException, InterruptedException {
        ReportDecision decision = policy.decide(event);
        if (decision.action() != ReportDecision.Action.SEND_REPORT) {
            return new Outcome(decision.action(), decision.auditReason(), null);
        }

        byte[] pdf = pdfReport.render(event, decision);
        String reportData = Base64.getEncoder().encodeToString(pdf);
        String html = "<h1>Payment learning report</h1><p>Your payment for <strong>"
                + escapeHtml(event.courseName()) + "</strong> is settled.</p><p><a download=\"payment-report.pdf\" href=\"data:application/pdf;base64,"
                + reportData + "\">Download the PDF report</a></p><p>Reference: " + escapeHtml(event.paymentId()) + "</p>";
        String key = "payment-report-" + sha256(event.paymentId() + ":" + event.status());
        InfraiEmailClient.SendReceipt receipt = emailClient.send(event.learnerEmail(),
                "Payment report for " + event.courseName(), html, key);
        return new Outcome(decision.action(), decision.auditReason(), receipt.messageId());
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Outcome(ReportDecision.Action action, String auditReason, String messageId) { }
}

