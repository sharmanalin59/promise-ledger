# Tech Stack Decision

## Backend

- Java 21
- Spring Boot
- MySQL (primary DB)
- Flyway migrations
- Docker
- Maven

## Data Strategy

- Store raw earnings call transcripts in MySQL (LONGTEXT)
- Extracted promises stored in relational tables
- Optional JSON column for AI metadata caching

## Primary Key Strategy

- UUID primary keys for all domain entities

Entities:

- Company
- Transcript
- Promise
- Evidence
- QuarterCredibilityScore

## Quarter Representation

Use:

- fiscal_year
- fiscal_quarter

instead of storing quarter as a single string.

### Reason

Improves sorting, querying, uniqueness constraints, and future analytics.

## Promise Tracking

Promise contains:

- status
- progress_percent (nullable)

### Purpose

- Track current promise completion
- Improve UI visibility
- Enable future scoring enhancements

### MVP Note

`progress_percent` is informational in MVP and does not affect credibility scoring.

## Evidence Sources

Supported evidence sources:

- FINANCIAL_STATEMENT
- EXCHANGE_FILING
- INVESTOR_PRESENTATION
- EARNINGS_CALL

### MVP Note

Evidence confidence is stored for transparency and future enhancements but is not used in credibility scoring.

## Reason

PromiseLedger is a relational, time-evolving system centered around:

- Companies
- Promises
- Evidence
- Quarterly credibility scores

A relational model provides strong consistency, auditability, and historical tracking while keeping future analytical features straightforward to implement.