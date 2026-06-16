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
- timeframe
- status (OPEN / DELIVERED / PARTIAL / MISSED)

### Evidence
- id
- promise_id
- source_type (TRANSCRIPT / FINANCIAL / FILINGS)
- text
- confidence