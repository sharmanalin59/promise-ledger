# System Workflow

1. Upload transcript
2. LLM extracts measurable promises
3. Store in DB
4. Next quarter:
   - ingest new transcript
   - match old promises
   - update status
5. Financial data used for verification
6. progress_percent is informational in MVP
   MVP scoring:
    DELIVERED = 1.0
    PARTIAL = 0.5

Rule:
Numbers > transcripts > opinions
