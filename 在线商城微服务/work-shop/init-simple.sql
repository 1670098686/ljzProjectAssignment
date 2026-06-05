CREATE DATABASE IF NOT EXISTS rjjg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rjjg;

-- 用户表
CREATE TABLE IF NOT EXISTS user_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

-- 插入测试用户
INSERT INTO user_user (username, password_hash, phone, status, created_at, updated_at) VALUES
('admin', '$2a$10$eW7oQ.3X1hW4c7FyIYl7nuyx8Lb9Z8R4uK5oX1eX7y7y7y7y7y7y7', '13800138000', 1, NOW(), NOW()),
('user1', '$2a$10$eW7oQ.3X1hW4c7FyIYl7nuyx8Lb9Z8R4uK5oX1eX7y7y7y7y7y7', '13800138001', 1, NOW(), NOW());
