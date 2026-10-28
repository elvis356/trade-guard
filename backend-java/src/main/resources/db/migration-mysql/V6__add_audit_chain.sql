CREATE TABLE audit_chain_state (
    id BIGINT PRIMARY KEY,
    last_hash VARCHAR(64) NOT NULL,
    CONSTRAINT chk_audit_chain_state_singleton CHECK (id = 1)
);

INSERT INTO audit_chain_state (id, last_hash)
VALUES (1, REPEAT('0', 64));

CREATE TABLE audit_entries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    actor VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    detail VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    prev_hash VARCHAR(64) NOT NULL,
    row_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE INDEX idx_audit_entries_created ON audit_entries (created_at, id);
