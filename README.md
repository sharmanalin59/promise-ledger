# PromiseLedger

PromiseLedger is a management accountability platform for public companies.

Investors can easily access earnings call transcripts, financial statements, investor presentations, and AI-generated summaries.

What they cannot easily access is:

* What management promised
* Whether management delivered
* How management credibility changes over time

PromiseLedger extracts measurable commitments from earnings call transcripts, tracks them across quarters, verifies outcomes using financial statements and disclosures, and builds a historical management credibility record.

---

## The Problem

Retail investors following 20–50 stocks do not have time to:

* Read every earnings call transcript
* Remember promises made several quarters ago
* Verify whether management delivered
* Track management credibility over time

Institutional investors often perform this analysis manually.

PromiseLedger automates it.

---

## How It Works

1. Ingest earnings call transcripts
2. Extract measurable management commitments
3. Store commitments as promises
4. Track promises across future quarters
5. Evaluate outcomes using evidence
6. Calculate management credibility dynamically from evaluated promises

### Promise Lifecycle

* OPEN
* PARTIAL
* DELIVERED
* MISSED
* EXPIRED

### Evidence Sources (priority order)

1. `FINANCIAL_STATEMENT`
2. `EXCHANGE_FILING`
3. `INVESTOR_PRESENTATION`
4. `EARNINGS_CALL`

Credibility is computed from evaluated promises (`DELIVERED`, `PARTIAL`, `MISSED`).  
If no evaluated promises exist, credibility is `NOT_AVAILABLE` (not zero).

Numbers beat words.

---

## MVP Scope

Initial company coverage:

* KPIT Technologies
* Coforge
* Newgen Software
* Persistent Systems
* LTIMindtree

MVP goals:

* Automatic promise extraction
* Cross-quarter promise tracking
* Outcome classification
* Management credibility scoring

Success criterion:

* At least 80% promise classification accuracy

---

## Tech Stack

### Backend

* Java 21
* Spring Boot
* MYSQL 8
* Flyway
* Docker

### Frontend

* React
* TypeScript
* Tailwind

### AI

* Claude API

---

## Running Locally

Prerequisites: JDK 21 or newer (`JAVA_HOME` set) and Docker. Maven is not required; the backend ships with the Maven wrapper.

Start MySQL (from the repository root):

```powershell
docker compose up -d --wait
```

If port 3306 is already in use (for example by a local MySQL server), choose another host port and use the same value when starting the backend:

```powershell
$env:DB_PORT = "3307"
docker compose up -d --wait
```

Run the tests (from `backend/`; Docker must be running because tests start their own MySQL container):

```powershell
.\mvnw.cmd test
```

Start the backend (from `backend/`). It applies the Flyway migrations and listens on `http://localhost:8080`:

```powershell
.\mvnw.cmd spring-boot:run
```

Database settings are read from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`. The defaults match `docker-compose.yml` and are for local development only.

---

## API

### Submit a transcript

`POST /api/transcripts` stores pasted earnings-call transcript text for one company and fiscal quarter. It does not extract promises; extraction is a separate, later step.

| Field | Rules |
|---|---|
| `companyTicker` | Required. One of `COFORGE`, `KPITTECH`, `LTIM`, `NEWGEN`, `PERSISTENT` (case-insensitive). |
| `fiscalYear` | Required. 2000–2100. |
| `fiscalQuarter` | Required. 1–4. |
| `text` | Required, not blank, at most 500,000 characters. |

```bash
curl -X POST http://localhost:8080/api/transcripts \
  -H 'Content-Type: application/json' \
  -d '{"companyTicker": "KPITTECH", "fiscalYear": 2026, "fiscalQuarter": 1, "text": "We expect revenue growth of 15% next year."}'
```

`201 Created`:

```json
{"id":"028273ed-6149-439c-a2ee-8ff495cbbd43","companyTicker":"KPITTECH","companyName":"KPIT Technologies","fiscalYear":2026,"fiscalQuarter":1,"textLength":42,"createdAt":"2026-10-14T12:28:32.523802Z"}
```

Errors use the RFC 9457 problem-details format (`application/problem+json`):

| Status | When |
|---|---|
| `400` | Missing, blank, out-of-range, or oversized fields (listed in `errors`); malformed JSON or wrong field types; unknown `companyTicker` (the response lists supported tickers). |
| `409` | A transcript already exists for that company, fiscal year, and quarter. |
| `415` | The request body is not `application/json`. |

```json
{"detail":"One or more fields are invalid.","instance":"/api/transcripts","status":400,"title":"Bad Request","errors":[{"field":"fiscalQuarter","message":"must be between 1 and 4"},{"field":"text","message":"is required and must not be blank"}]}
```

---

## Documentation

Detailed specifications are available in the `/docs` directory:

* Product Vision
* MVP Scope
* Domain Model
* Workflow
* Technical Architecture

See `CLAUDE.md` for AI agent instructions.
