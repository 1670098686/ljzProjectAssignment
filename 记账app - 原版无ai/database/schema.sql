CREATE DATABASE IF NOT EXISTS flutter CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE flutter;

-- Drop existing tables to keep the script idempotent
DROP TABLE IF EXISTS outbox_events;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS savings;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS user_profile;

CREATE TABLE user_profile (
    user_id BIGINT NOT NULL PRIMARY KEY,
    nickname VARCHAR(30) NOT NULL,
    remind_time VARCHAR(10),
    backup_status DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    icon VARCHAR(100) NOT NULL DEFAULT 'default_icon.png',
    type INT NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_categories_user_name_type (user_id, name, type),
    KEY idx_categories_user (user_id),
    KEY idx_categories_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE budgets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    monthly_budget DECIMAL(10,2) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories (id),
    UNIQUE KEY uk_budgets_user_category_month (user_id, category_id, year, month),
    KEY idx_budgets_user (user_id),
    KEY idx_budgets_user_category (user_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type INT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_date DATE NOT NULL,
    remark VARCHAR(100),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES categories (id),
    KEY idx_transactions_user_date (user_id, transaction_date),
    KEY idx_transactions_user_type_category (user_id, type, category_id),
    KEY idx_user_recent (user_id, transaction_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE savings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(10,2) NOT NULL,
    current_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    deadline DATE NOT NULL,
    description VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_savings_user (user_id),
    KEY idx_savings_user_deadline (user_id, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    exchange_name VARCHAR(100) NOT NULL,
    routing_key VARCHAR(150) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error LONGTEXT,
    available_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_outbox_status_available (status, available_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
