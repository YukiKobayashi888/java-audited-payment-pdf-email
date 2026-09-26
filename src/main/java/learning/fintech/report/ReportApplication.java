package learning.fintech.report;

import java.time.Instant;

public final class ReportApplication {
    private ReportApplication() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: ReportApplication learner@example.com");
            System.exit(64);
        }

        ReportConfiguration configuration = ReportConfiguration.fromEnvironment();
        PaymentReportService service = new PaymentReportService(
                new ReportPolicy(70), new AuditPdfReport(), new InfraiEmailClient(configuration));
        PaymentReportController controller = new PaymentReportController(service);
        PaymentEvent event = new PaymentEvent("pay_course_2048", args[0], "Practical Ledger Design",
                12900, "USD", PaymentEvent.Status.SETTLED, 18, Instant.parse("2026-09-24T08:30:00Z"));

        PaymentReportController.ServiceResponse response = controller.submit(event);
        System.out.printf("status=%d state=%s audit_reason=%s message_id=%s%n",
                response.status(), response.state(), response.auditReason(), response.messageId());
        if (response.status() >= 400) System.exit(1);
    }
}

