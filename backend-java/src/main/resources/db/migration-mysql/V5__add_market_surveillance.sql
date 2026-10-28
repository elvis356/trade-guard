CREATE TABLE surveillance_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alert_key VARCHAR(120) NOT NULL UNIQUE,
    rule_code VARCHAR(32) NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    severity VARCHAR(8) NOT NULL,
    trade_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_alert_instrument FOREIGN KEY (symbol) REFERENCES instruments(symbol),
    CONSTRAINT fk_alert_trade FOREIGN KEY (trade_id) REFERENCES trades(id)
);

CREATE INDEX idx_surveillance_alerts_occurred
    ON surveillance_alerts (occurred_at DESC, id DESC);

CREATE TABLE surveillance_cases (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL UNIQUE,
    proposed_by VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reviewed_by VARCHAR(64),
    review_note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6),
    CONSTRAINT chk_surveillance_case_status CHECK (
        status IN ('PROPOSED', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_surveillance_case_review CHECK (
        (status = 'PROPOSED' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR (status IN ('APPROVED', 'REJECTED')
            AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    ),
    CONSTRAINT fk_case_alert FOREIGN KEY (alert_id) REFERENCES surveillance_alerts(id)
);

CREATE INDEX idx_surveillance_cases_created
    ON surveillance_cases (created_at DESC, id DESC);
