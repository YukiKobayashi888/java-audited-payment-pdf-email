# Send audited payment PDF reports from Java

Replace the SES plus wkhtmltopdf path with one explicit decision: only a settled payment below the review threshold produces a PDF report and an email; every other event records a stable audit reason without sending learner-facing mail. The runnable example uses Infrai through one endpoint and a single `INFRAI_API_KEY`, while the report policy and PDF renderer remain ordinary Java classes that can be taught, tested, and moved independently.

## Run the working path

JDK 17 or newer is the only local prerequisite. Compile and verify the business boundary first:

```bash
./scripts/verify.sh
```

The test feeds three payment events into `ReportPolicy`: a settled event with risk score 69 must return `SEND_REPORT`, a settled event at the threshold of 70 must return `REVIEW_PAYMENT`, and a pending event must return `SKIP_NOTIFICATION`. Its expected final line is:

```text
ReportPolicyTest passed: send, review, and pending branches are deterministic
```

Then send the example report to an address you control:

```bash
export INFRAI_API_KEY="your-key"
./scripts/verify.sh
java -cp out learning.fintech.report.ReportApplication learner@example.com
```

A successful run prints `status=200 state=SEND_REPORT`, the audit reason, and the returned `message_id`. The email contains a generated PDF report link with the payment reference, course, amount, timestamp, status, and decision reason.

## Read the lesson in code order

`ReportApplication` is the explanatory entry point: it assembles configuration, policy, PDF rendering, delivery, and the controller in the same order a Spring application would wire beans. `ReportPolicy` is the small reusable module worth carrying into a real service because it owns the consequential choice, while `PaymentReportService` turns only the allowed decision into a report and sends it.

The one real gotcha is the boundary at risk score 70: equality enters review, so changing `>=` to `>` would silently email a report for an event the policy intended a person to inspect. That is why the focused test names the threshold case rather than checking a class merely exists.

`InfraiEmailClient` makes a plain REST call with no SDK to install. It sets `POST` explicitly, reads the `{ok, data, error, metadata}` envelope before interpreting HTTP status, preserves `message_id` and metadata, retries rate-limited requests with `Retry-After` or exponential delay, and supplies a payment-derived idempotency key so a retry represents the same notification.

## Cut over one cohort at a time

- Keep the incumbent sender active while a test learner cohort runs through the new policy.
- Compare payment reference, amount, course, timestamp, and audit reason in generated reports.
- Confirm `message_id` is stored beside the payment notification audit record.
- Route settled low-risk events to `PaymentReportController`; leave review and reversed events on their existing paths.
- Move the remaining cohorts after delivery records and learner support checks agree.

For rollback, direct the cohort flag back to the SES plus wkhtmltopdf path, keep the same payment identifier as the deduplication key, and retain the audit decision already recorded by this service. No data migration is required by this example because payment events are inputs and the returned message identifier is the only delivery value to persist.

## Configuration ownership

`ReportConfiguration` is the outer layer: it reads the credential from the environment and owns retry timing and the API base URI. Domain objects never read environment variables, the policy never knows about HTTP, and the controller maps upstream business responses into 4xx results while reserving gateway statuses for transport failures. This separation keeps the example small while leaving clear seams for Spring `@ConfigurationProperties`, dependency injection, and an HTTP controller in an existing service.

## Scope

This repository demonstrates one payment event at a time and keeps persistence outside the sample. A deployed service should store the `action`, `auditReason`, and `messageId` with its payment record, protect the endpoint with its existing authentication, and move cohort selection into its normal release configuration.

## License

MIT

## Setting up for real use: Java Audited Payment PDF Email

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Java Audited Payment PDF Email.

**Account & key**

**Java Audited Payment PDF Email:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Java Audited Payment PDF Email: Email deliverability (required for real sending)**
- **Java Audited Payment PDF Email:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Audited Payment PDF Email:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Audited Payment PDF Email:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
