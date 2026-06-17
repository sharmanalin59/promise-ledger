# System Workflow

1. Upload transcript
2. LLM extracts measurable promises
3. Store in DB
4. Next quarter:
   - ingest new transcript
   - match old promises
   - update status
5. Financial data used for verification

Rule:
Numbers > transcripts > opinions



## Future Enhancements

### Confidence-Weighted Promise Evaluation

In MVP, promise status and company credibility are determined using:

- DELIVERED = 1.0
- PARTIAL = 0.5
- MISSED = 0

Evidence confidence is displayed to users but does not affect scoring.

Future versions may incorporate evidence confidence into promise evaluation and credibility calculations.

Example:

Promise Completion = 75%
Evidence Confidence = 90%

Weighted Completion = 67.5%

This would allow PromiseLedger to distinguish between:
- Strongly verified outcomes supported by financial statements and filings
- Weakly supported outcomes based primarily on management commentary

The exact weighting model will be determined after MVP validation and real-world testing.