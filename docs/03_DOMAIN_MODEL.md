# Domain Model

## Core Entities

### Company
- id
- name
- ticker

### Transcript
- id
- company_id
- quarter
- text

### Promise
- id
- company_id
- transcript_id
- statement
- category
- importance
- progress_percent
- timeframe
- status (OPEN / DELIVERED / PARTIAL / MISSED)

### Evidence
- id
- promise_id
- source_type (TRANSCRIPT / FINANCIAL / FILINGS)
- text
- confidence

### CredibilitySnapshot
-------------------
id
company_id
quarter
score
delivered_count
partial_count
missed_count
created_at