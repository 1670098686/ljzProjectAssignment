CREATE TABLE idempotency_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner VARCHAR(100) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) DEFAULT NULL,
    response_body LONGTEXT DEFAULT NULL,
    error_message VARCHAR(1024) DEFAULT NULL,
    status VARCHAR(20) NOT NULL,
    expire_at DATETIME NOT NULL,
    completed_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotent_owner_key (owner, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
