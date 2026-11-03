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

Claude settings (needed only for promise extraction; set them in the environment, never in files):

| Variable | Default | Purpose |
|---|---|---|
| `ANTHROPIC_API_KEY` | none | Claude API key. Without it, extraction returns `503`. |
| `CLAUDE_MODEL` | `claude-sonnet-5-5` | Model ID. |
| `CLAUDE_TIMEOUT` | `120s` | Maximum time to wait for Claude's reply. |
| `CLAUDE_MAX_TOKENS` | `16000` | Maximum reply length. |

The automated tests mock Claude and need no API key. One optional test calls the real Claude API (and incurs API charges); it runs only when both variables are set:

```powershell
$env:ANTHROPIC_API_KEY = "<your key>"
$env:CLAUDE_LIVE_TEST = "true"
.\mvnw.cmd test -Dtest=ClaudeLiveExtractionTests
```

On networks that intercept HTTPS, Java may fail with `PKIX path building failed`. On Windows, add `"-DargLine=-Djavax.net.ssl.trustStoreType=Windows-ROOT"` to the test command so the test JVM trusts the Windows certificate store.

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

### Extract promises from a transcript

`POST /api/transcripts/{id}/extract` sends a stored transcript to Claude, validates the reply, and saves the extracted promises with status `OPEN`. It runs synchronously and can take up to `CLAUDE_TIMEOUT`.

```bash
curl -X POST http://localhost:8080/api/transcripts/<transcript id>/extract
```

`200 OK` returns `transcriptId`, `promiseCount`, and `promises`, where each promise has `id`, `statement`, `sourceQuote`, `category`, `importance` (`HIGH`/`MEDIUM`/`LOW`), `timeframe` (or omitted), and `status`. A transcript with no promises returns `promiseCount: 0`.

Claude's reply is accepted only if it is a JSON object with a `promises` list in which every item has a non-blank `statement` and `sourceQuote` (each at most 2,000 characters), an `importance` of `HIGH`, `MEDIUM`, or `LOW`, an optional `category` (at most 100 characters) and `timeframe` (at most 255 characters), and a `sourceQuote` that appears in the transcript (ignoring differences in whitespace, letter case, and curly versus straight quotes and dashes). If any item fails, nothing is saved. The prompt is in `backend/src/main/resources/prompts/promise-extraction.txt`.

| Status | When |
|---|---|
| `404` | The transcript does not exist. |
| `409` | Promises have already been extracted for the transcript. |
| `502` | Claude returned an error, an empty or malformed reply, or promises that failed validation (listed in `errors`). Nothing is saved. |
| `503` | `ANTHROPIC_API_KEY` is not configured. |
| `504` | Claude did not reply within `CLAUDE_TIMEOUT`. Nothing is saved. |
| `500` | The promises could not be saved. Nothing is saved. |

The transcript stays stored after any failure, so extraction can be retried.

---

## Documentation

Detailed specifications are available in the `/docs` directory:

* Product Vision
* MVP Scope
* Domain Model
* Workflow
* Technical Architecture

See `CLAUDE.md` for AI agent instructions.
