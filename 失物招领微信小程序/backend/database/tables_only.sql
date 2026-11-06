-- 校园失物招领小程序数据库表结构
-- 仅表结构部分
-- 表结构与实际数据库完全一致

USE `lost_and_found`;

-- 用户表
CREATE TABLE `users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(64) NOT NULL,
  `unionid` VARCHAR(64) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `password` VARCHAR(255) DEFAULT NULL,
  `nickname` VARCHAR(64) DEFAULT NULL,
  `avatar_url` VARCHAR(255) DEFAULT NULL,
  `role` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
  `phone` VARCHAR(32) DEFAULT NULL,
  `signature` VARCHAR(200) DEFAULT NULL,
  `email_verified` TINYINT(1) DEFAULT 0,
  `status` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `openid` (`openid`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 邮箱验证码表
CREATE TABLE `email_verification_codes` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(128) NOT NULL,
  `code` VARCHAR(10) NOT NULL,
  `purpose` ENUM('REGISTER', 'RESET_PASSWORD', 'BIND_EMAIL') NOT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  `used` TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email` (`email`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 物品信息表
CREATE TABLE `items` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `item_type` ENUM('LOST', 'FOUND') NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `category` VARCHAR(32) NOT NULL,
  `happen_time` TIMESTAMP NOT NULL,
  `location` VARCHAR(128) NOT NULL,
  `feature_desc` TEXT DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `contact_wechat` VARCHAR(128) DEFAULT NULL,
  `audit_status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  `biz_status` ENUM('ONGOING', 'RESOLVED', 'RETURNED', 'CANCELLED') DEFAULT 'ONGOING',
  `view_count` INT DEFAULT 0,
  `is_locked` TINYINT(1) DEFAULT 0,
  `visible` TINYINT(1) DEFAULT 1,
  `published_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type` (`item_type`),
  KEY `idx_audit` (`audit_status`),
  KEY `idx_biz` (`biz_status`),
  KEY `idx_happen_time` (`happen_time`),
  KEY `idx_published` (`published_at`),
  FULLTEXT KEY `idx_search` (`title`, `location`, `feature_desc`),
  CONSTRAINT `items_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 物品图片表
CREATE TABLE `item_images` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `item_id` INT NOT NULL,
  `image_url` VARCHAR(255) NOT NULL,
  `sort` INT DEFAULT 0,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  CONSTRAINT `item_images_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 会话表
CREATE TABLE `conversations` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user1_id` INT NOT NULL,
  `user2_id` INT NOT NULL,
  `item_id` INT DEFAULT NULL,
  `last_message_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pair_item` (`user1_id`, `user2_id`, `item_id`),
  KEY `idx_user1` (`user1_id`),
  KEY `idx_user2` (`user2_id`),
  KEY `idx_last_message` (`last_message_at`),
  KEY `idx_item` (`item_id`),
  CONSTRAINT `conversations_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`),
  CONSTRAINT `conversations_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`),
  CONSTRAINT `conversations_ibfk_3` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 消息表
CREATE TABLE `messages` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `conversation_id` INT NOT NULL,
  `sender_id` INT NOT NULL,
  `type` ENUM('TEXT', 'IMAGE') NOT NULL DEFAULT 'TEXT',
  `content` TEXT DEFAULT NULL,
  `image_url` VARCHAR(255) DEFAULT NULL,
  `status` ENUM('SENT', 'READ') DEFAULT 'SENT',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conversation` (`conversation_id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_created` (`created_at`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知表
CREATE TABLE `notifications` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `type` ENUM('AUDIT', 'SYSTEM', 'MESSAGE') NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NOT NULL,
  `is_read` TINYINT(1) DEFAULT 0,
  `related_id` INT DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_read` (`is_read`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 举报表
CREATE TABLE `reports` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `item_id` INT NOT NULL,
  `reporter_id` INT NOT NULL,
  `reason_type` ENUM('SCAM', 'AD', 'SENSITIVE', 'OTHER') NOT NULL,
  `reason` TEXT DEFAULT NULL,
  `status` ENUM('PENDING', 'PROCESSED', 'REJECTED') DEFAULT 'PENDING',
  `action` ENUM('REMOVE', 'REJECT', 'IGNORE') DEFAULT NULL,
  `handler_id` INT DEFAULT NULL,
  `handle_result` TEXT DEFAULT NULL,
  `handled_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_reporter` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_handler` (`handler_id`),
  CONSTRAINT `reports_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `reports_ibfk_2` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `reports_ibfk_3` FOREIGN KEY (`handler_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 审核记录表
CREATE TABLE `audit_logs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `item_id` INT NOT NULL,
  `auditor_id` INT NOT NULL,
  `action` ENUM('APPROVE', 'REJECT') NOT NULL,
  `reason` TEXT DEFAULT NULL,
  `audited_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_auditor` (`auditor_id`),
  CONSTRAINT `audit_logs_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `audit_logs_ibfk_2` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认管理员账号（密码：admin123，实际使用时请生成真实的 bcrypt 哈希值）
INSERT INTO `users` (`openid`, `email`, `password`, `nickname`, `role`, `email_verified`, `status`)
VALUES ('admin_openid', 'admin@school.edu.cn', '$2b$10$YourHashedPasswordHere', '系统管理员', 'ADMIN', 1, 1);
