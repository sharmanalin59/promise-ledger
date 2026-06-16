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
6. Calculate management credibility

### Promise Lifecycle

* OPEN
* PARTIAL
* DELIVERED
* MISSED
* EXPIRED

### Evidence Priority

1. Financial Statements
2. Exchange Filings
3. Investor Presentations
4. Earnings Calls

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
* PostgreSQL
* Flyway
* Docker

### Frontend

* React
* TypeScript
* Tailwind

### AI

* Claude API

---

## Documentation

Detailed specifications are available in the `/docs` directory:

* Product Vision
* MVP Scope
* Domain Model
* Workflow
* Technical Architecture

See `CLAUDE.md` for AI agent instructions.
