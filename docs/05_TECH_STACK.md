# Tech Stack Decision

## Backend

- Java 21
- Spring Boot
- MySQL (primary DB)
- Flyway migrations
- Docker
- Maven

## Data Strategy

- Store raw earnings call transcripts in MySQL (LONGTEXT)
- Extracted promises stored in relational tables
- Optional JSON column for AI metadata caching

## Primary Key Strategy

- UUID primary keys for all domain entities

Entities:

- Company
- Transcript
- Promise
- Evidence

## Quarter Representation

Use:

- fiscal_year
- fiscal_quarter

instead of storing quarter as a single string.

### Reason

Improves sorting, querying, uniqueness constraints, and future analytics.

## Promise Tracking

Promise is linked to its originating transcript via `transcript_id`.

Company ownership is derived as:

```text
Promise → Transcript → Company
```

Promise does not store `company_id`.

Promise contains:

- transcript_id
- status
- progress_percent (nullable DECIMAL, range 0.00 - 100.00)
- timeframe (nullable free-text)

### Purpose

- Track current promise completion
- Improve UI visibility
- Enable future scoring enhancements

### MVP Notes

- `progress_percent` is informational in MVP and does not affect credibility scoring.
- `timeframe` is nullable free-text in MVP. Structured timeframe support is future scope.

## Evidence Sources

Supported evidence sources:

- FINANCIAL_STATEMENT
- EXCHANGE_FILING
- INVESTOR_PRESENTATION
- EARNINGS_CALL

### Distinction

- `Transcript` is a domain entity that stores the actual earnings call transcript.
- `EARNINGS_CALL` is an evidence `source_type` used when supporting evidence comes from an earnings call.

### MVP Notes

- Evidence `confidence` is a decimal in the range 0.0000 - 1.0000.
- Confidence is stored for transparency and future enhancements but is not used in credibility scoring.

## Credibility Scoring

MVP credibility is computed dynamically from evaluated promises:

- DELIVERED = 1.0
- PARTIAL = 0.5
- MISSED = 0.0
- OPEN and EXPIRED are excluded from the score denominator

Evaluated promise counts (`delivered_count`, `partial_count`, `missed_count`, `evaluated_count`) are dynamically derived from current promise statuses for display and auditability. They are not persisted in the MVP and are not domain-model fields.

Score behavior:

- when `evaluated_count = 0`, `credibility_score` is `null` / `NOT_AVAILABLE` (not `0.0`)
- when `evaluated_count > 0`, `credibility_score` is a decimal in the range 0.0000 - 1.0000

There is no `QuarterCredibilityScore` entity in the MVP. Persisted credibility snapshots are future scope.

## Reason

PromiseLedger is a relational, time-evolving system centered around:

- Companies
- Promises
- Evidence
- Dynamically computed credibility scores

A relational model provides strong consistency, auditability, and historical tracking while keeping future analytical features straightforward to implement.

In the MVP, "historical tracking" means following each promise's status and progress across later quarters. It does not mean storing historical `PromiseEvaluation` records. Historical evaluation and audit records remain future scope.