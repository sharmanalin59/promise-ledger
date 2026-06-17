

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


### Credibility to include delay of execution:
Credibility scoring may incorporate delivery timing penalties.
Promises delivered after the stated deadline may receive reduced credit.