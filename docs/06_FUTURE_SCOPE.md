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

# 8A. Structured Timeframe Support

## Problem

The MVP stores `Promise.timeframe` as nullable free-text.

This is flexible for early extraction, but makes deadline comparison and delay analysis difficult.

Example free-text values:

* "by Q2 FY26"
* "within 12 months"
* "over the next few quarters"

## Future Enhancement

Introduce structured timeframe fields.

Potential fields:

* target_fiscal_year
* target_fiscal_quarter
* target_date
* timeframe_text (retain original free-text for auditability)

Purpose:

* Enable deterministic deadline checks
* Support timing-aware credibility penalties
* Improve promise matching and reporting

This enhancement is intentionally excluded from the MVP.

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

# 10. Structured Financial Metrics Repository

## Problem

The MVP evaluates promises using evidence records entered manually during verification.

Financial statements, investor presentations and management commentary are not stored as structured financial data.

As a result, numerical verification depends on evidence text rather than direct metric comparison.

Example:

Promise:

"Reduce debt by ₹100 crore by Q2."

Evidence:

"Net debt reduced from ₹500 crore to ₹380 crore."

The system stores the evidence but not the underlying financial metrics separately.

## Future Enhancement

Introduce a structured FinancialMetric entity.

Potential fields:

* id
* company_id
* fiscal_year
* fiscal_quarter
* metric_name
* metric_value
* source_type
* created_at

Example:

* NET_DEBT = 380
* REVENUE = 1200
* EBITDA_MARGIN = 18.5
* CASH_BALANCE = 950

Purpose:

* Enable automated promise verification
* Support historical financial analysis
* Reduce dependence on manually entered evidence text
* Improve credibility calculation accuracy

This enhancement is intentionally excluded from the MVP.

---

# 11. Persisted Credibility Scores

The MVP has no `QuarterCredibilityScore` entity.

In the MVP:

* credibility is computed dynamically from current promise statuses
* evaluated promise counts are derived at read time and are not persisted
* when `evaluated_count = 0`, credibility is `null` / `NOT_AVAILABLE`

Future versions may persist credibility snapshots for:

* faster dashboard loading
* historical trend analysis
* reporting

Any future snapshot entity is intentionally excluded from the MVP and must not be confused with the removed `QuarterCredibilityScore` domain-model concept.

---

# Guiding Principle

Future enhancements should only be implemented after validating that the core PromiseLedger workflow works reliably:

Transcript → Promise → Evidence → Evaluation → Credibility

The MVP must prove this workflow before additional complexity is introduced.