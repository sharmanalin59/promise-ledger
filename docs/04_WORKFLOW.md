# System Workflow

## MVP Workflow

1. User uploads or pastes an earnings call transcript
2. LLM extracts measurable promises from the transcript (AI-assisted)
3. Validated promises are stored with status `OPEN`
4. Next quarter:
   - upload or paste the new transcript
   - match old promises
   - update status
5. Verification: the user manually enters or attaches `Evidence` records to a promise
   - examples: a financial result, an exchange filing, an investor-presentation reference, or an earnings-call excerpt
   - each record has a `source_type`, `text`, and optional `confidence`
   - evidence is not automatically retrieved from financial APIs or other external sources
6. The promise's `status` (and optional `progress_percent`) is updated based on that evidence
   - `progress_percent` is informational in MVP

## Future Scope (not MVP)

- Automated financial-data retrieval
- Automated evidence discovery
- Automated evidence extraction from financial statements, filings, or presentations

## MVP Credibility Scoring

Weights:

- DELIVERED = 1.0
- PARTIAL = 0.5
- MISSED = 0.0

Inclusion rules:

- Only evaluated promises are included in the score denominator:
  - DELIVERED
  - PARTIAL
  - MISSED
- OPEN and EXPIRED are excluded from the score denominator.

Formula:

```text
credibility_score =
  (delivered_count * 1.0 + partial_count * 0.5 + missed_count * 0.0)
  / evaluated_count
```

Where:

- `evaluated_count = delivered_count + partial_count + missed_count`
- `delivered_count`, `partial_count`, `missed_count`, and `evaluated_count` are dynamically derived from current promise statuses
- these counts are not persisted in the MVP
- when `evaluated_count = 0`, `credibility_score` is `null` / `NOT_AVAILABLE` (do not treat this as `0.0` credibility)
- when `evaluated_count > 0`, `credibility_score` is a decimal in the range 0.0000 - 1.0000

Rule:
Numbers > transcripts > opinions
