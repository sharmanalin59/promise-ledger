# Domain Model

## Core Entities

### Company
- id
- name
- ticker

### Transcript
- id
- company_id
- fiscal_year
- fiscal_quarter
- text

### Promise
- id
- company_id
- transcript_id
- statement
- category
- importance
- progress_percent (nullable DECIMAL)
- timeframe
- status (OPEN / DELIVERED / PARTIAL / MISSED / EXPIRED)

### Evidence
- id
- promise_id
- source_type (TRANSCRIPT / FINANCIAL / FILINGS)
- text
- confidence

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