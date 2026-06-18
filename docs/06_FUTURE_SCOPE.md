# Future Scope

This document contains enhancements intentionally excluded from the MVP.

The goal of the MVP is to validate that management commitments can be extracted, tracked, evaluated, and presented as a credibility ledger.

Items in this document should not influence MVP implementation unless explicitly approved.

---

# 1. Timing-Aware Credibility

## Problem

The MVP treats all delivered promises equally regardless of delivery timing.

Example:

Promise:

"Reduce debt by ₹100 crore by Q2."

Outcomes:

* Company A delivers in Q2.
* Company B delivers in Q4.

Both are currently marked as DELIVERED.

## Future Enhancement

Apply delivery timing penalties.

Example:

* Delivered on time = 1.0
* Delivered 1 quarter late = 0.8
* Delivered 2 quarters late = 0.6

Purpose:

* Reward execution speed.
* Distinguish timely delivery from delayed delivery.

---

# 2. Confidence-Weighted Evaluation

## Problem

MVP credibility scoring ignores evidence confidence.

A promise supported by audited financial statements is treated the same as a promise supported only by management commentary.

## Future Enhancement

Incorporate evidence confidence into promise evaluation.

Example:

Completion = 80%

Evidence Confidence = 90%

Weighted Completion = 72%

Purpose:

* Reward stronger evidence.
* Reduce reliance on weak or ambiguous sources.

---

# 3. Quarterly Promise History

## Problem

The MVP stores only the current state of a promise.

Historical progress is not preserved.

Example:

Q2 = 50%

Q3 = 75%

Q4 = 100%

Only the latest state is retained.

## Future Enhancement

Store quarterly promise evaluations.

Potential benefits:

* Execution velocity tracking
* Delay analysis
* Historical progress visualization
* Better credibility calculations

---

# 4. Promise Evaluation History

## Problem

The MVP stores only the latest status and progress of a promise.

Example:

Q2:

* Status = PARTIAL
* Progress = 50%

Q3:

* Status = PARTIAL
* Progress = 75%

Q4:

* Status = DELIVERED
* Progress = 100%

Historical evaluation states are overwritten.

## Future Enhancement

Introduce a PromiseEvaluation entity.

Potential fields:

* promise_id
* fiscal_year
* fiscal_quarter
* status
* progress_percent
* evaluation_reasoning
* created_at

Purpose:

* Preserve historical promise evaluations
* Track execution progress over time
* Support delay-aware credibility models
* Support execution velocity analytics

---

# 5. Leadership Attribution

## Problem

MVP credibility is calculated only at the company level.

Management changes are not considered.

## Future Enhancement

Track credibility separately for:

* Company
* CEO
* CFO

Example:

Company Credibility = 72%

Current CEO Credibility = 88%

Purpose:

* Reflect leadership changes.
* Avoid penalizing new management for historical failures.

---

# 6. Forecast Accuracy Tracking

## Problem

Management often makes verifiable forward-looking statements that are not explicit promises.

Examples:

* "This is a one-time exceptional item."
* "Margins should normalize next quarter."
* "Demand weakness is temporary."

These statements influence investor trust but are not tracked in the MVP.

## Future Enhancement

Track forecast and guidance accuracy.

Potential categories:

* Margin guidance
* Demand outlook
* Exceptional items
* Industry commentary
* Management explanations
* One-time event explanations
* Forward-looking management statements

Purpose:

* Measure management reliability.
* Complement promise-based credibility scoring.

---

# 7. Advanced Credibility Model

Potential future inputs:

* Promise completion
* Delivery timing
* Evidence confidence
* Forecast accuracy
* Management tenure

Purpose:

Create a more sophisticated management credibility score than the MVP status-based model.

---

# 8. Additional Evidence Sources

## Future Enhancement

Support additional evidence sources beyond the MVP.

Potential sources:

* Annual reports
* Investor day presentations
* Regulatory disclosures
* Press releases
* Conference presentations

Purpose:

* Improve promise verification coverage
* Increase evaluation confidence
* Reduce dependence on a single source type

---

# 9. Product Features Beyond MVP

Potential future features:

* Watchlists
* Portfolio integration
* Alerts and notifications
* AI-powered company summaries
* Company comparison dashboards
* Management credibility rankings
* Sector-level credibility analytics

These features are intentionally excluded from MVP scope.

---

# Guiding Principle

Future enhancements should only be implemented after validating that the core PromiseLedger workflow works reliably:

Transcript → Promise → Evidence → Evaluation → Credibility

The MVP must prove this workflow before additional complexity is introduced.