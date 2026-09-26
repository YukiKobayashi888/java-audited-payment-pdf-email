package learning.fintech.report;

import java.time.Instant;

public final class ReportPolicyTest {
    public static void main(String[] args) {
        ReportPolicy policy = new ReportPolicy(70);
        PaymentEvent safe = event("safe", PaymentEvent.Status.SETTLED, 69);
        PaymentEvent threshold = event("review", PaymentEvent.Status.SETTLED, 70);
        PaymentEvent pending = event("pending", PaymentEvent.Status.PENDING, 10);

        assertEquals(ReportDecision.Action.SEND_REPORT, policy.decide(safe).action(), "settled low-risk payment");
        assertEquals(ReportDecision.Action.REVIEW_PAYMENT, policy.decide(threshold).action(), "threshold boundary");
        assertEquals("risk_review_required", policy.decide(threshold).auditReason(), "auditable risk reason");
        assertEquals(ReportDecision.Action.SKIP_NOTIFICATION, policy.decide(pending).action(), "pending payment");
        System.out.println("ReportPolicyTest passed: send, review, and pending branches are deterministic");
    }

    private static PaymentEvent event(String id, PaymentEvent.Status status, int riskScore) {
        return new PaymentEvent(id, "learner@example.com", "Ledger Basics", 2500, "USD", status,
                riskScore, Instant.parse("2026-09-24T08:30:00Z"));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}

