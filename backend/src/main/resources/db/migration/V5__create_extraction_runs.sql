-- One row per extract call: provider outcome, token usage, and estimated cost.
-- Promises stay unchanged when the call fails; this row is how that call is inspected.
CREATE TABLE extraction_runs (
    id                  CHAR(36)       NOT NULL,
    transcript_id       CHAR(36)       NOT NULL,
    provider            VARCHAR(40)    NOT NULL,
    model               VARCHAR(100)   NOT NULL,
    attempt_count       INT            NOT NULL,
    input_tokens        INT            NULL,
    output_tokens       INT            NULL,
    estimated_cost_usd  DECIMAL(12, 6) NULL,
    latency_ms          BIGINT         NOT NULL,
    failure_category    VARCHAR(40)    NULL,
    status              VARCHAR(20)    NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    CONSTRAINT pk_extraction_runs PRIMARY KEY (id),
    CONSTRAINT fk_extraction_runs_transcript FOREIGN KEY (transcript_id) REFERENCES transcripts (id),
    CONSTRAINT chk_extraction_runs_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_extraction_runs_failure_category CHECK (
        failure_category IS NULL OR failure_category IN (
            'TIMEOUT', 'RATE_LIMITED', 'TRANSIENT', 'CLIENT_ERROR', 'CIRCUIT_OPEN', 'INPUT_TOO_LARGE',
            'TRUNCATED', 'UNREADABLE', 'NOT_CONFIGURED', 'BUSY', 'PERSISTENCE', 'INVALID_OUTPUT'
        )
    ),
    INDEX idx_extraction_runs_transcript_id (transcript_id)
);
