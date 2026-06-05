CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator VARCHAR(100),
    ip VARCHAR(64),
    endpoint VARCHAR(200),
    http_method VARCHAR(10),
    title VARCHAR(100),
    action VARCHAR(100),
    type VARCHAR(30),
    result VARCHAR(20),
    error_message VARCHAR(500),
    resource_id VARCHAR(100),
    request_params TEXT,
    request_body TEXT,
    response TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE INDEX idx_operation_logs_operator ON operation_logs (operator);
CREATE INDEX idx_operation_logs_create_time ON operation_logs (create_time);
