CREATE TABLE eod_reports (
    business_date DATE PRIMARY KEY,
    trade_count BIGINT NOT NULL,
    xml_content TEXT NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    generated_at DATETIME(6) NOT NULL
);
