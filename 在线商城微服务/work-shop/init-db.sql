-- 创建数据库
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

-- 地址表
CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    detail VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES user_user(id)
);

-- 商品分类表
CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    level TINYINT,
    sort INT,
    created_at DATETIME,
    updated_at DATETIME
);

-- 商品表
CREATE TABLE IF NOT EXISTS product_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    category_id BIGINT,
    stock INT,
    product_image VARCHAR(255),
    description TEXT,
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

-- 库存表
CREATE TABLE IF NOT EXISTS stock_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNIQUE NOT NULL,
    available_qty INT,
    locked_qty INT,
    version INT,
    updated_at DATETIME,
    FOREIGN KEY (product_id) REFERENCES product_product(id)
);

-- 商品库存操作幂等日志（防止重复扣减/回滚）
CREATE TABLE IF NOT EXISTS product_stock_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_order_product_action (order_id, product_id, action_type),
    KEY idx_order_action (order_id, action_type),
    KEY idx_product_action (product_id, action_type)
);

-- 购物车表
CREATE TABLE IF NOT EXISTS cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    checked BOOLEAN DEFAULT TRUE,
    unit_price DECIMAL(10,2),
    product_name VARCHAR(100),
    product_image VARCHAR(255),
    total_price DECIMAL(10,2),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_user_product (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES user_user(id),
    FOREIGN KEY (product_id) REFERENCES product_product(id)
);

-- 订单表
CREATE TABLE IF NOT EXISTS order_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2),
    freight DECIMAL(10,2),
    final_amount DECIMAL(10,2),
    status VARCHAR(20),
    payment_type VARCHAR(20),
    address_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME,
    payment_time DATETIME,
    delivery_time DATETIME,
    receive_time DATETIME,
    cancel_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES user_user(id),
    FOREIGN KEY (address_id) REFERENCES user_address(id)
);

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    price DECIMAL(10,2),
    quantity INT,
    amount DECIMAL(10,2),
    product_name VARCHAR(100),
    product_image VARCHAR(255),
    FOREIGN KEY (order_id) REFERENCES order_order(id),
    FOREIGN KEY (product_id) REFERENCES product_product(id)
);

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_user_product (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES user_user(id),
    FOREIGN KEY (product_id) REFERENCES product_product(id)
);

-- 评价表
CREATE TABLE IF NOT EXISTS review_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    rating TINYINT,
    content TEXT,
    images VARCHAR(500),
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES user_user(id),
    FOREIGN KEY (product_id) REFERENCES product_product(id),
    FOREIGN KEY (order_id) REFERENCES order_order(id)
);

-- Seata 事务日志表
CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    UNIQUE KEY uk_undo_log (xid, branch_id)
);
