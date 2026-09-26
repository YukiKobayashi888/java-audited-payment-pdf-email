package learning.fintech.report;

public final class ReportPolicy {
    private final int reviewThreshold;

    public ReportPolicy(int reviewThreshold) {
        this.reviewThreshold = reviewThreshold;
    }

    public ReportDecision decide(PaymentEvent event) {
        if (event.status() == PaymentEvent.Status.REVERSED) {
            return new ReportDecision(ReportDecision.Action.SKIP_NOTIFICATION, "payment_reversed");
        }
        if (event.riskScore() >= reviewThreshold) {
            return new ReportDecision(ReportDecision.Action.REVIEW_PAYMENT, "risk_review_required");
        }
        if (event.status() != PaymentEvent.Status.SETTLED) {
            return new ReportDecision(ReportDecision.Action.SKIP_NOTIFICATION, "payment_not_settled");
        }
        return new ReportDecision(ReportDecision.Action.SEND_REPORT, "settled_below_risk_threshold");
    }
}

