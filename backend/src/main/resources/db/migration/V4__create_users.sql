CREATE TABLE users (
    id            CHAR(36)     NOT NULL,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);
