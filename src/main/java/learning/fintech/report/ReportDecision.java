package learning.fintech.report;

public record ReportDecision(Action action, String auditReason) {
    public enum Action { SEND_REPORT, REVIEW_PAYMENT, SKIP_NOTIFICATION }
}

