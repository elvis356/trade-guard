CREATE TABLE eod_runs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    business_date DATE NOT NULL,
    completed BOOLEAN NOT NULL,
    orders_checked BIGINT NOT NULL,
    trades_checked BIGINT NOT NULL,
    events_checked BIGINT NOT NULL,
    issues_json TEXT NOT NULL,
    report_sha256 VARCHAR(64),
    generated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_eod_runs_business_date
    ON eod_runs (business_date, generated_at DESC, id DESC);
