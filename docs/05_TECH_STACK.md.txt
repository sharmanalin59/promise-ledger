
# Tech Stack Decision

Backend:
- Java 21
- Spring Boot
- MySQL (primary DB)

Data Strategy:
- Store raw transcripts in MySQL (LONGTEXT)
- Extracted promises stored in relational tables
- Optional JSON column for AI metadata caching

Reason:
System is relational and time-evolving, not document-based.