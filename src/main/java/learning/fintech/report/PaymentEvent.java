package learning.fintech.report;

import java.time.Instant;
import java.util.Objects;

public record PaymentEvent(
        String paymentId,
        String learnerEmail,
        String courseName,
        long amountMinor,
        String currency,
        Status status,
        int riskScore,
        Instant occurredAt) {

    public PaymentEvent {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(learnerEmail);
        Objects.requireNonNull(courseName);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(status);
        Objects.requireNonNull(occurredAt);
        if (amountMinor < 0 || riskScore < 0 || riskScore > 100) {
            throw new IllegalArgumentException("amount and risk score are outside their accepted range");
        }
    }

    public enum Status { SETTLED, PENDING, REVERSED }
}

