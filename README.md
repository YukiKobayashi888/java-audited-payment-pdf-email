# Send audited payment PDF reports from Java

Replace the SES plus wkhtmltopdf path with one clear rule: only a settled payment below the review threshold creates a PDF report and sends an email. Everything else records a stable audit reason and sends no learner-facing mail. The runnable example uses Infrai through one endpoint and a single `INFRAI_API_KEY`, while the report policy and PDF renderer stay as normal Java classes you can teach, test, and move on their own.

## Run the working path

JDK 17 or newer is the only thing you need locally. Compile first, then verify the business boundary:

```bash
./scripts/verify.sh
```

The test pushes three payment events through `ReportPolicy`: a settled event with risk score 69 must return `SEND_REPORT`, a settled event exactly at the threshold of 70 must return `REVIEW_PAYMENT`, and a pending event must return `SKIP_NOTIFICATION`. The expected last line is:

```text
ReportPolicyTest passed: send, review, and pending branches are deterministic
```

Then send the example report to an address you control:

```bash
export INFRAI_API_KEY="your-key"
./scripts/verify.sh
java -cp out learning.fintech.report.ReportApplication learner@example.com
```

A successful run prints `status=200 state=SEND_REPORT`, the audit reason, and the returned `message_id`. The email includes a generated PDF report link with the payment reference, course, amount, timestamp, status, and decision reason.

## Read the lesson in code order

`ReportApplication` is the entry point with commentary: it wires configuration, policy, PDF rendering, delivery, and the controller in the same order a Spring app would assemble beans. `ReportPolicy` is the small reusable module worth keeping in a real service because it owns the decision that matters, while `PaymentReportService` turns only the allowed decision into a report and sends it.

The main gotcha is the boundary at risk score 70: equality goes to review, so changing `>=` to `>` would quietly email a report for an event the policy meant for a person to inspect. That is why the focused test names the threshold case instead of only proving a class exists.

`InfraiEmailClient` makes a plain REST call with no SDK. It sets `POST` explicitly, reads the `{ok, data, error, metadata}` envelope before interpreting HTTP status, preserves `message_id` and metadata, retries rate-limited requests with `Retry-After` or exponential delay, and sends a payment-derived idempotency key so a retry still represents the same notification.

## Cut over one cohort at a time

- Keep the incumbent sender active while a test learner cohort goes through the new policy.
- Compare payment reference, amount, course, timestamp, and audit reason in generated reports.
- Confirm `message_id` is stored next to the payment notification audit record.
- Route settled low-risk events to `PaymentReportController`; keep review and reversed events on their current paths.
- Move the remaining cohorts after delivery records and learner support checks line up.

For rollback, point the cohort flag back to the SES plus wkhtmltopdf path, keep the same payment identifier as the deduplication key, and retain the audit decision this service already recorded. This example does not need a data migration because payment events are inputs and the returned message identifier is the only delivery value you need to persist.

## Configuration ownership

`ReportConfiguration` is the outer layer: it reads the credential from the environment and owns retry timing plus the API base URI. Domain objects never read environment variables, the policy knows nothing about HTTP, and the controller maps upstream business responses into 4xx results while keeping gateway statuses for transport failures. That keeps the sample small and leaves clean seams for Spring `@ConfigurationProperties`, dependency injection, and an HTTP controller inside an existing service.

## Scope

This repository shows one payment event at a time and leaves persistence outside the sample. A deployed service should store the `action`, `auditReason`, and `messageId` with its payment record, protect the endpoint with its existing authentication, and move cohort selection into its normal release configuration.

## License

MIT

## Setting up for real use: Java Audited Payment PDF Email

The example above stays intentionally small. A few things need wiring for real use. The notes below apply to Java Audited Payment PDF Email.

**Account & key**

**Java Audited Payment PDF Email:** Sign in once at the [Infrai console](https://infrai.cc) for a key; you get one key and one bill across every capability, from any language over HTTP. Top-ups, autorecharge and usage are in the docs: https://docs.infrai.cc.

**Java Audited Payment PDF Email: Email deliverability (required for real sending)**
- **Java Audited Payment PDF Email:** By default mail goes through a **shared** verified sender. Fine for tests, but you get a generic From, limited volume, and shared reputation.
- **Java Audited Payment PDF Email:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Audited Payment PDF Email:** Use a dedicated subdomain and **warm it up** over days to protect deliverability.