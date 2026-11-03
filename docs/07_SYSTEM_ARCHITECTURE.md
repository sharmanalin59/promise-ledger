# 07 — SYSTEM ARCHITECTURE

## 1. Purpose

This document defines the technical architecture of PromiseLedger.

PromiseLedger is an AI-assisted financial intelligence system that tracks management commitments over time and evaluates whether those commitments were fulfilled.

The core principle of the architecture is:

> **Use LLMs for semantic understanding. Use deterministic software for business rules, persistence, scoring, and workflow orchestration.**

PromiseLedger is not designed as an autonomous agentic system. The application controls the workflow.

---

# 2. High-Level Architecture

```text
                         ┌─────────────────────┐
                         │   React Frontend    │
                         │ React + TypeScript  │
                         └──────────┬──────────┘
                                    │
                                  REST
                                    │
                         ┌──────────▼──────────┐
                         │   Spring Boot API   │
                         │                     │
                         │ Application Layer   │
                         └──────────┬──────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
       ┌─────────────┐      ┌──────────────┐      ┌─────────────┐
       │   MySQL     │      │ Claude / LLM │      │ External    │
       │             │      │              │      │ Data APIs   │
       └─────────────┘      └──────────────┘      └─────────────┘
              ▲                                            │
              │                                            │
              └────────────────────────────────────────────┘
                         Scheduled Ingestion
```

MVP note: transcripts enter the system only through manual upload or text paste via the Spring Boot API. The External Data APIs and Scheduled Ingestion shown above are future scope.

---

# 3. Architectural Principle

PromiseLedger separates the system into two categories.

## 3.1 Deterministic Application Responsibilities

The application is responsible for:

- Transcript ingestion via manual upload or text paste
- Storing evidence records entered manually during verification
- Persisting data
- Promise lifecycle management
- Duplicate detection
- Status updates
- Credibility score calculation
- API responses
- Error handling
- Preserving raw transcripts and evidence for traceability

These operations should not depend on an LLM.

The MVP keeps current promise state only. Historical evaluation and audit records are future scope.

Out of MVP scope:

- Authentication
- Notifications
- Scheduled ingestion and calls to external transcript or financial-data APIs
- Automated evidence retrieval, discovery, or extraction

---

## 3.2 LLM Responsibilities

The LLM is used only where semantic interpretation is required.

Primary responsibilities:

- Identifying management commitments
- Extracting structured promises from transcripts
- Identifying measurable targets
- Extracting target timelines
- Determining whether a statement represents a promise
- Semantic matching between historical promises and statements in new transcripts
- Assisting with interpretation of ambiguous evidence that the user has entered

In the MVP, the LLM does not retrieve, discover, or extract evidence.

Example:

```text
Management Statement
        │
        ▼
"We expect margins to reach 23% by Q4."
        │
        ▼
LLM
        │
        ▼
Structured Promise

metric: operating_margin
target_value: 23
operator: >=
target_period: Q4
```

Once this structured output exists, the rest should primarily be handled by deterministic application logic.

---

# 4. Core System Modules

The Spring Boot backend should be divided into logical modules.

```text
com.promiseledger
│
├── company
├── transcript
├── ingestion
├── promise
├── extraction
├── evaluation
├── evidence
├── credibility
├── financial
├── notification
└── common
```

`financial` and `notification` are future scope and have no MVP implementation role. The MVP does not include a financial module or the `FinancialMetric` entity (see `06_FUTURE_SCOPE.md` §10).

---

# 5. Company Module

Responsible for company-level data.

Responsibilities:

- Company creation
- Company profile
- Company identifiers
- Listing companies
- Company-level credibility metrics

Example:

```text
Company
   │
   ├── Transcripts
   │      └── Promises (via transcript_id)
   │             └── Evidence
   └── Credibility (computed dynamically in MVP)
```

---

# 6. Transcript Module

Responsible for storing management communications.

Supported sources may eventually include:

- Earnings call transcripts
- Investor presentations
- Annual reports
- Quarterly reports
- Management interviews

For the MVP, the only source is earnings call transcripts, supplied by manual upload or text paste. Fetching transcripts from external sources is future scope.

MVP workflow:

```text
Manual upload / text paste
      │
      ▼
Transcript Ingestion
      │
      ▼
Raw Transcript
      │
      ▼
Database
      │
      ▼
Extraction Processing
```

The original source data should be preserved for auditability.

---

# 7. Promise Extraction Module

This is the primary AI boundary.

```text
Transcript
    │
    ▼
PromiseExtractionService
    │
    ▼
LLM Provider
    │
    ▼
Structured JSON
    │
    ▼
Validation
    │
    ▼
Promise
```

The LLM should return structured output rather than free-form text.

Conceptually:

```json
{
  "isPromise": true,
  "statement": "We expect operating margins to reach 23% by Q4.",
  "metric": "operating_margin",
  "targetValue": 23,
  "operator": "GREATER_THAN_OR_EQUAL",
  "targetPeriod": "Q4 FY26",
  "confidence": 0.94
}
```

The application validates the response before persisting it.

MVP note: the implemented extraction output contains only `statement`, `sourceQuote`, `category`, `importance`, and `timeframe` (see the README and `backend/src/main/resources/prompts/promise-extraction.txt`). Structured targets such as `metric`, `targetValue`, `operator`, and `targetPeriod` are future scope.

---

# 8. Promise Ledger Module

The Promise Ledger is the core domain component.

Each promise has a lifecycle:

```text
OPEN
  │
  ├──► DELIVERED
  ├──► PARTIAL
  ├──► MISSED
  └──► EXPIRED
```

A promise is created with status `OPEN` once extraction output has been validated and persisted. Extraction is a processing step, not a `Promise.status` value.

Status may be updated in later quarters as new evidence is evaluated.

`docs/03_DOMAIN_MODEL.md` is the authoritative definition of Promise statuses.

The MVP stores the current promise state only. When a promise is re-evaluated, its `status` and `progress_percent` are updated in place.

Persisting historical promise evaluations is future scope.

---

# 9. Evidence Module

Evidence represents information used to evaluate a promise.

MVP evidence `source_type` values:

- FINANCIAL_STATEMENT
- EXCHANGE_FILING
- INVESTOR_PRESENTATION
- EARNINGS_CALL

Distinction:

- The `Transcript` entity stores the actual earnings call transcript content.
- `EARNINGS_CALL` identifies that a piece of evidence came from an earnings call.

Evidence `confidence` is a nullable decimal in the range 0.0000 - 1.0000 and is informational in MVP.

MVP evidence entry:

- Evidence records are entered manually by the user during verification and attached to a promise.
- Examples: a financial result, an exchange filing, an investor-presentation reference, or an earnings-call excerpt.
- Evidence is not automatically retrieved from financial APIs or other external sources.
- Automated financial-data retrieval, evidence discovery, and evidence extraction are future scope.

Architecture:

```text
Promise
   │
   ├── Evidence 1
   ├── Evidence 2
   └── Evidence 3
```

Each evaluation should be traceable to evidence.

Example:

```text
Promise:
"Operating margin will reach 23%."

Evidence:
Q4 financial results

Actual:
23.4%

Evaluation:
DELIVERED
```

---

# 10. Evaluation Module

The evaluation module determines whether a promise has been fulfilled.

MVP flow:

```text
Historical Promise
        │
        ▼
Evidence entered manually by the user
        │
        ▼
Evaluation
        │
        ▼
Update Promise status / progress_percent
```

In the MVP, the outcome of an evaluation is stored on the `Promise` itself (current state only), and supporting information is stored as `Evidence` rows.

Future-only flow (not part of the MVP):

```text
Evaluation
        │
        ▼
PromiseEvaluation (historical evaluation record)
```

Where possible, evaluation should be deterministic.

Example:

```text
Target: Margin >= 23%

Actual: Margin = 23.4%

23.4 >= 23

Result: DELIVERED
```

For ambiguous or qualitative promises, the LLM may assist with evidence interpretation.

In the MVP, an evaluation persists:

- `Promise.status`
- `Promise.progress_percent` (informational)
- `Evidence` rows (`source_type`, `text`, `confidence`)

Future scope: a `PromiseEvaluation` record could additionally store evaluation date, explanation, and per-evaluation confidence. See `06_FUTURE_SCOPE.md` §4.

---

# 11. Credibility Module

Credibility is calculated from historical promise performance.

MVP scoring weights:

```text
DELIVERED = 1.0
PARTIAL   = 0.5
MISSED    = 0.0
```

Inclusion rules:

- Only evaluated promises are included in the denominator: DELIVERED, PARTIAL, MISSED
- OPEN and EXPIRED are excluded from the score denominator

The calculation itself must remain deterministic.

Conceptually:

```text
credibility_score =
  (delivered_count * 1.0 + partial_count * 0.5 + missed_count * 0.0)
  / evaluated_count
```

Where:

- `evaluated_count = delivered_count + partial_count + missed_count`
- evaluated promise counts are dynamically derived from current promise statuses and are not persisted in the MVP
- when `evaluated_count = 0`, `credibility_score` is `null` / `NOT_AVAILABLE` (do not treat this as `0.0` credibility)
- when `evaluated_count > 0`, `credibility_score` is a decimal in the range 0.0000 - 1.0000

There is no `QuarterCredibilityScore` entity in the MVP. Persisted credibility snapshots are future scope.

The important architectural requirement is that credibility calculations should be reproducible.

---

# 12. Data Ingestion Architecture

## MVP

Transcripts are ingested manually by upload or text paste through the Spring Boot API (`POST /api/transcripts`).

The MVP has no scheduler, ingestion jobs, queues, or external data APIs.

## Future Scope

Automated ingestion is future scope. When it is introduced, data collection should be separated from the API request path, and the frontend should not trigger expensive ingestion workflows.

Possible future flow:

```text
Scheduler
    │
    ▼
Ingestion Job
    │
    ▼
External API
    │
    ▼
Validate
    │
    ▼
Persist Raw Data
    │
    ▼
Queue / Processing
```

Possible future AWS implementation:

```text
EventBridge Schedule
        │
        ▼
Ingestion Service
        │
        ▼
External Financial APIs
        │
        ▼
Database
```

---

# 13. Processing Workflow

The primary workflow should be:

```text
1. Transcript uploaded or pasted (manual)
        │
        ▼
2. Store transcript
        │
        ▼
3. Start extraction
        │
        ▼
4. Call Claude API
        │
        ▼
5. Receive structured promises
        │
        ▼
6. Validate output
        │
        ▼
7. Persist promises
        │
        ▼
8. Display in Promise Ledger
```

MVP verification workflow:

```text
1. Select an existing promise
        │
        ▼
2. User enters or attaches Evidence
        │
        ▼
3. Evaluate promise
        │
        ▼
4. Update Promise status / progress_percent
        │
        ▼
5. Credibility recomputed on read
```

Future evaluation workflow (not MVP):

```text
Financial Data Arrives
        │
        ▼
Identify Relevant Historical Promises
        │
        ▼
Retrieve Evidence
        │
        ▼
Evaluate Promise
        │
        ▼
Store Evaluation History
        │
        ▼
Recalculate Credibility
```

---

# 14. Frontend Architecture

The React frontend should be a separate presentation layer.

```text
React + TypeScript
        │
        ▼
API Client Layer
        │
        ▼
Spring Boot REST API
```

The frontend should not contain:

- Promise extraction logic
- Credibility calculation logic
- Financial evaluation rules
- LLM prompts

Those responsibilities belong to the backend.

Primary pages:

```text
/
Dashboard

/companies
Company List

/companies/{id}
Company Profile

/promises
Promise Ledger

/promises/{id}
Promise Detail

/transcripts
Transcript Management
```

---

# 15. Backend API Architecture

Initial REST API categories:

```text
/api/companies

/api/transcripts

/api/promises

/api/evaluations

/api/credibility
```

Example:

```text
GET  /api/companies

GET  /api/companies/{id}

GET  /api/companies/{id}/promises

GET  /api/promises/{id}

GET  /api/promises/{id}/evaluations

POST /api/transcripts

POST /api/transcripts/{id}/extract
```

`/api/evaluations` and `GET /api/promises/{id}/evaluations` depend on `PromiseEvaluation` and are future scope, not part of the MVP.

The detailed API contract should be documented separately.

---

# 16. Database Architecture

MySQL is the system of record.

Conceptually:

```text
Company
   │
   └──── Transcript
            │
            └──── Promise
                     │
                     └──── Evidence
```

MVP relationship:

```text
Promise → Transcript → Company
```

Promise stores `transcript_id` only. It does not store `company_id`.

`PromiseEvaluation` and persisted credibility snapshots are future scope and are not MVP tables.

Key principle:

> Raw source data should be preserved.

The MVP stores the current promise state. Historical evaluation history is future scope.

---

# 17. AI Provider Architecture

For the MVP, `PromiseExtractionService` uses `ClaudeClient` directly:

```text
PromiseExtractionService
        │
        ▼
ClaudeClient
        │
        ▼
Claude API
```

The MVP does not require a generic `LLMClient` interface or multiple provider implementations. This follows `CLAUDE.md`: simplicity over abstraction.

Future scope: if a second LLM provider is ever needed, a provider interface can be introduced at that point.

---

# 18. Failure Handling

Important failure cases:

## LLM Failure

```text
Claude API unavailable
        │
        ▼
Extraction marked FAILED
        │
        ▼
Retry according to policy
```

The transcript must remain stored even if extraction fails.

MVP note: extraction runs synchronously and no extraction status is persisted. A failed extraction returns an error to the caller, is logged, and saves no promises; retrying means calling the extract endpoint again. Stored statuses and automatic retries are future scope.

---

## Invalid LLM Output

```text
LLM response
      │
      ▼
Schema validation
      │
      ├── Valid → Persist
      │
      └── Invalid → Retry / Failed
```

Never directly trust unvalidated LLM output.

---

## Duplicate Data

In the MVP, a second transcript for the same company, fiscal year, and fiscal quarter is rejected.

Duplicate detection using source identifiers or content metadata applies to automated ingestion and is future scope.

---

## External API Failure (future scope)

Applies once external data APIs are introduced. The system should:

- Log failures
- Preserve error information
- Retry transient failures
- Avoid duplicate ingestion

---

# 19. Security

Secrets must never be stored in source code.

Sensitive values include:

- Claude API keys
- Database credentials

MVP approach:

```text
Docker / local development
    │
    └── Environment Variables
```

Future scope: external API keys, AWS credentials, and AWS Secrets Manager / Parameter Store apply only if external APIs or an AWS production deployment are introduced.

---

# 20. Observability

For the MVP:

- Application logs
- Error logs
- API failure logging
- LLM request status
- Extraction status

Scheduled job status becomes relevant only once automated ingestion (future scope) is introduced.

Important events:

```text
Transcript ingested

Extraction started

Extraction completed

Extraction failed

Promise created

Promise evaluated

Credibility recalculated
```

---

# 21. Deployment Architecture

## MVP

The MVP is a simple Docker-based monolith, consistent with `05_TECH_STACK.md`:

```text
Docker Compose
    │
    ├── Spring Boot application (single container)
    │
    └── MySQL 8 (container)
```

No AWS services are required to implement or run the MVP.

## Future / Production Considerations (not MVP)

A possible future AWS production deployment:

```text
                    Internet
                       │
                       ▼
                CloudFront
                       │
                       ▼
                  React App
                   (S3)
                       │
                       ▼
                 Spring Boot API
                       │
                       ▼
                     MySQL
                     (RDS)
```

Future supporting services for automated ingestion (not MVP):

```text
Scheduled Jobs
      │
      ▼
EventBridge
      │
      ▼
Application Processing
```

Secrets:

```text
AWS Secrets Manager
        │
        ▼
Spring Boot
        │
        ▼
Claude API
```

If an AWS production deployment is pursued later, the compute platform for Spring Boot can be decided then.

Potential options:

- ECS Fargate
- EC2
- Elastic Beanstalk

For the MVP, simplicity should be prioritized over unnecessary infrastructure complexity.

---

# 22. Explicit Non-Agentic Design

The MVP workflow is application-controlled.

```text
Application decides:
    │
    ├── What transcript to process
    ├── When to call the LLM
    ├── What data to provide
    ├── When to persist results
    ├── How to calculate scores
    └── When to evaluate promises
```

The LLM does not autonomously control the application.

Therefore:

> **MCP is not required for the PromiseLedger MVP.**

> **An autonomous agent is not required for the PromiseLedger MVP.**

---

# 23. Future Agentic Extension

A future bounded agent could be introduced for open-ended evidence discovery.

Example:

```text
Unresolved Promise
        │
        ▼
Verification Agent
        │
        ├── Search Financial Statements
        ├── Search Investor Presentations
        ├── Search Historical Disclosures
        │
        ▼
Evidence Collection
        │
        ▼
Proposed Evaluation
```

This is intentionally outside the MVP.

---

# 24. Final Architectural Summary

PromiseLedger follows this model:

```text
                    PROMISELEDGER

         ┌─────────────────────────────┐
         │        React Frontend       │
         └──────────────┬──────────────┘
                        │
                        ▼
         ┌─────────────────────────────┐
         │      Spring Boot API        │
         │                             │
         │  Company                    │
         │  Transcript                 │
         │  Promise                    │
         │  Evaluation                 │
         │  Credibility                │
         │  Ingestion                  │
         └───────┬─────────────┬───────┘
                 │             │
                 ▼             ▼
             MySQL           Claude
                                │
                          Semantic Tasks
                                │
                    Promise Extraction
                    Semantic Matching
                    Evidence Interpretation
```

The fundamental design principle is:

> **LLM for understanding. Application code for control. Database for truth.**

This architecture provides a clear path for building the MVP while keeping the system auditable, maintainable, and extensible.
