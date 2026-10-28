CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_app_users_role
        CHECK (role IN ('TRADER', 'SURVEILLANCE', 'AUDITOR', 'ADMIN'))
);

CREATE INDEX idx_app_users_enabled ON app_users (enabled, username);
