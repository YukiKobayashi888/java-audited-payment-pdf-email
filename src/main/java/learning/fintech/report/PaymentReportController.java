package learning.fintech.report;

import java.io.IOException;

public final class PaymentReportController {
    private final PaymentReportService service;

    public PaymentReportController(PaymentReportService service) {
        this.service = service;
    }

    public ServiceResponse submit(PaymentEvent event) {
        try {
            PaymentReportService.Outcome outcome = service.handle(event);
            int status = outcome.action() == ReportDecision.Action.REVIEW_PAYMENT ? 202 : 200;
            return new ServiceResponse(status, outcome.action().name(), outcome.auditReason(), outcome.messageId());
        } catch (InfraiEmailClient.InfraiException error) {
            int status = error.statusCode() >= 400 && error.statusCode() < 500 ? error.statusCode() : 502;
            return new ServiceResponse(status, "EMAIL_REJECTED", error.code(), null);
        } catch (IOException error) {
            return new ServiceResponse(502, "EMAIL_TRANSPORT_ERROR", error.getMessage(), null);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return new ServiceResponse(503, "REQUEST_INTERRUPTED", "delivery interrupted", null);
        }
    }

    public record ServiceResponse(int status, String state, String auditReason, String messageId) { }
}

