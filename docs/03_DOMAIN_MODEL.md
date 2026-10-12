# Domain Model

## Core Entities

### Company
- id
- name
- ticker
- created_at
- updated_at

### Transcript
- id
- company_id
- fiscal_year
- fiscal_quarter
- text
- created_at
- updated_at

The `Transcript` entity stores the actual earnings call transcript content.

It is not an evidence source enum value. When evidence is derived from an earnings call, the evidence `source_type` is `EARNINGS_CALL`.

### Promise
- id
- transcript_id
- statement
- category
- importance (HIGH / MEDIUM / LOW)
- progress_percent (nullable DECIMAL, range 0.00 - 100.00)
- timeframe (nullable free-text; structured timeframe support is future scope)
- status (OPEN / DELIVERED / PARTIAL / MISSED / EXPIRED)
- created_at
- updated_at

`transcript_id` identifies the originating transcript that produced the promise.

Company ownership is derived through:

```text
Promise → Transcript → Company
```

Promise does not store `company_id`.

### Evidence
- id
- promise_id
- source_type (FINANCIAL_STATEMENT / EXCHANGE_FILING / INVESTOR_PRESENTATION / EARNINGS_CALL)
- text
- confidence (nullable DECIMAL, range 0.0000 - 1.0000)
- created_at
- updated_at

In the MVP, evidence records are entered manually during verification. Automated evidence retrieval is future scope.

## Relationships

```text
Company 1 ─── * Transcript
Transcript 1 ─── * Promise
Promise 1 ─── * Evidence
```

MVP tables:

- companies
- transcripts
- promises
- evidence

Credibility scores and evaluated promise counts are computed dynamically and are not persisted.
