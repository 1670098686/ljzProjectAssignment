-- 校园失物招领小程序数据库初始化脚本
-- 表结构与实际数据库完全一致

-- 创建数据库
CREATE DATABASE IF NOT EXISTS lost_and_found DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lost_and_found;

-- 1. 用户表
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) UNIQUE NOT NULL COMMENT '微信 openid',
    unionid VARCHAR(64) COMMENT '微信 unionid',
    email VARCHAR(128) UNIQUE COMMENT '学业邮箱',
    password VARCHAR(255) COMMENT '密码（加密存储）',
    nickname VARCHAR(64) COMMENT '昵称',
    avatar_url VARCHAR(255) COMMENT '头像 URL',
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '角色',
    phone VARCHAR(32) COMMENT '联系电话',
    signature VARCHAR(200) COMMENT '个性签名',
    email_verified TINYINT(1) DEFAULT 0 COMMENT '邮箱是否已验证',
    status TINYINT(1) DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 邮箱验证码表
DROP TABLE IF EXISTS email_verification_codes;
CREATE TABLE email_verification_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(128) NOT NULL COMMENT '邮箱地址',
    code VARCHAR(10) NOT NULL COMMENT '验证码',
    purpose ENUM('REGISTER', 'RESET_PASSWORD', 'BIND_EMAIL') NOT NULL COMMENT '用途',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    used TINYINT(1) DEFAULT 0 COMMENT '是否已使用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

-- 3. 物品信息表
DROP TABLE IF EXISTS items;
CREATE TABLE items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '发布者用户ID',
    item_type ENUM('LOST', 'FOUND') NOT NULL COMMENT '物品类型：LOST-寻物，FOUND-拾物',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    category VARCHAR(32) NOT NULL COMMENT '分类',
    happen_time TIMESTAMP NOT NULL COMMENT '丢失/拾到时间',
    location VARCHAR(128) NOT NULL COMMENT '地点',
    feature_desc TEXT COMMENT '特征描述',
    contact_name VARCHAR(64) COMMENT '联系人姓名',
    contact_phone VARCHAR(32) COMMENT '联系电话',
    contact_wechat VARCHAR(128) COMMENT '联系微信',
    audit_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING' COMMENT '审核状态',
    biz_status ENUM('ONGOING', 'RESOLVED', 'RETURNED', 'CANCELLED') DEFAULT 'ONGOING' COMMENT '业务状态',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    is_locked TINYINT(1) DEFAULT 0 COMMENT '是否锁定',
    visible TINYINT(1) DEFAULT 1 COMMENT '是否可见（上下架）',
    published_at TIMESTAMP COMMENT '发布时间（审核通过时设置）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_type (item_type),
    INDEX idx_audit (audit_status),
    INDEX idx_biz (biz_status),
    INDEX idx_happen_time (happen_time),
    INDEX idx_published (published_at),
    FULLTEXT INDEX idx_search (title, location, feature_desc),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物品信息表';

-- 4. 物品图片表
DROP TABLE IF EXISTS item_images;
CREATE TABLE item_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL COMMENT '物品ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片URL',
    sort INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id),
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物品图片表';

-- 5. 会话表
DROP TABLE IF EXISTS conversations;
CREATE TABLE conversations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user1_id INT NOT NULL COMMENT '参与者1',
    user2_id INT NOT NULL COMMENT '参与者2',
    item_id INT COMMENT '关联物品ID',
    last_message_at TIMESTAMP COMMENT '最后消息时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_last_message (last_message_at),
    UNIQUE KEY uk_user_pair_item (user1_id, user2_id, item_id),
    FOREIGN KEY (user1_id) REFERENCES users(id),
    FOREIGN KEY (user2_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 6. 消息表
DROP TABLE IF EXISTS messages;
CREATE TABLE messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL COMMENT '会话ID',
    sender_id INT NOT NULL COMMENT '发送者ID',
    type ENUM('TEXT', 'IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '消息类型',
    content TEXT COMMENT '消息内容',
    image_url VARCHAR(255) COMMENT '图片URL（图片消息时使用）',
    status ENUM('SENT', 'READ') DEFAULT 'SENT' COMMENT '消息状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_created (created_at),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 7. 通知表
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '接收者用户ID',
    type ENUM('AUDIT', 'SYSTEM', 'MESSAGE') NOT NULL COMMENT '通知类型',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    related_id INT COMMENT '关联ID(如物品ID)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_read (is_read),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 8. 举报表
DROP TABLE IF EXISTS reports;
CREATE TABLE reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL COMMENT '被举报物品ID',
    reporter_id INT NOT NULL COMMENT '举报者用户ID',
    reason_type ENUM('SCAM', 'AD', 'SENSITIVE', 'OTHER') NOT NULL COMMENT '举报原因类型',
    reason TEXT COMMENT '举报详情',
    status ENUM('PENDING', 'PROCESSED', 'REJECTED') DEFAULT 'PENDING' COMMENT '处理状态',
    action ENUM('REMOVE', 'REJECT', 'IGNORE') COMMENT '处理动作',
    handler_id INT COMMENT '处理者管理员ID',
    handle_result TEXT COMMENT '处理结果',
    handled_at TIMESTAMP COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id),
    INDEX idx_reporter (reporter_id),
    INDEX idx_status (status),
    INDEX idx_handler (handler_id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    FOREIGN KEY (handler_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 9. 审核记录表
DROP TABLE IF EXISTS audit_logs;
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL COMMENT '物品ID',
    auditor_id INT NOT NULL COMMENT '审核员ID',
    action ENUM('APPROVE', 'REJECT') NOT NULL COMMENT '审核动作',
    reason TEXT COMMENT '审核原因',
    audited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id),
    INDEX idx_auditor (auditor_id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (auditor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- 插入一个测试管理员账号（请自行修改密码）
-- 注意：密码需要使用 bcrypt 加密后存储
