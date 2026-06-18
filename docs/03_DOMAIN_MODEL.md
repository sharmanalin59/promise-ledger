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

### Promise
- id
- company_id
- transcript_id
- statement
- category
- importance (HIGH / MEDIUM / LOW)
- progress_percent (nullable DECIMAL, range (0.00 - 100.00))
- timeframe
- status (OPEN / DELIVERED / PARTIAL / MISSED / EXPIRED)
- created_at
- updated_at

### Evidence
- id
- promise_id
- source_type (TRANSCRIPT / FINANCIAL_STATEMENT / EXCHANGE_FILING / INVESTOR_PRESENTATION / EARNINGS_CALL)
- text
- confidence
- created_at
- updated_at

### QuarterCredibilityScore
-------------------
- id
- company_id
- fiscal_year
- fiscal_quarter
- score
- delivered_count
- partial_count
- missed_count
- created_at
- updated_at