CREATE TABLE companies (
    id         CHAR(36)     NOT NULL,
    name       VARCHAR(255) NOT NULL,
    ticker     VARCHAR(32)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uk_companies_name UNIQUE (name),
    CONSTRAINT uk_companies_ticker UNIQUE (ticker)
);

CREATE TABLE transcripts (
    id             CHAR(36)    NOT NULL,
    company_id     CHAR(36)    NOT NULL,
    fiscal_year    INT         NOT NULL,
    fiscal_quarter TINYINT     NOT NULL,
    text           LONGTEXT    NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_transcripts PRIMARY KEY (id),
    CONSTRAINT fk_transcripts_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uk_transcripts_company_period UNIQUE (company_id, fiscal_year, fiscal_quarter),
    CONSTRAINT chk_transcripts_fiscal_quarter CHECK (fiscal_quarter BETWEEN 1 AND 4)
);

CREATE TABLE promises (
    id               CHAR(36)     NOT NULL,
    transcript_id    CHAR(36)     NOT NULL,
    statement        TEXT         NOT NULL,
    category         VARCHAR(100) NULL,
    importance       VARCHAR(20)  NOT NULL,
    progress_percent DECIMAL(5,2) NULL,
    timeframe        VARCHAR(255) NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_promises PRIMARY KEY (id),
    CONSTRAINT fk_promises_transcript FOREIGN KEY (transcript_id) REFERENCES transcripts (id),
    CONSTRAINT chk_promises_importance CHECK (importance IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_promises_status CHECK (status IN ('OPEN', 'DELIVERED', 'PARTIAL', 'MISSED', 'EXPIRED')),
    CONSTRAINT chk_promises_progress_percent CHECK (
        progress_percent IS NULL OR (progress_percent >= 0.00 AND progress_percent <= 100.00)
    ),
    INDEX idx_promises_transcript_id (transcript_id)
);

CREATE TABLE evidence (
    id          CHAR(36)     NOT NULL,
    promise_id  CHAR(36)     NOT NULL,
    source_type VARCHAR(40)  NOT NULL,
    text        TEXT         NOT NULL,
    confidence  DECIMAL(5,4) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_evidence PRIMARY KEY (id),
    CONSTRAINT fk_evidence_promise FOREIGN KEY (promise_id) REFERENCES promises (id) ON DELETE CASCADE,
    CONSTRAINT chk_evidence_source_type CHECK (
        source_type IN ('FINANCIAL_STATEMENT', 'EXCHANGE_FILING', 'INVESTOR_PRESENTATION', 'EARNINGS_CALL')
    ),
    CONSTRAINT chk_evidence_confidence CHECK (
        confidence IS NULL OR (confidence >= 0.0000 AND confidence <= 1.0000)
    ),
    INDEX idx_evidence_promise_id (promise_id)
);
