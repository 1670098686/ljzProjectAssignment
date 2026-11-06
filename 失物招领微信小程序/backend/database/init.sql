-- =====================================================
-- 校园失物招领小程序 - 数据库初始化脚本
-- 数据库版本: MySQL 8.0+
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 存储引擎: InnoDB
-- 表结构与实际数据库完全一致
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `lost_and_found`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `lost_and_found`;

-- =====================================================
-- 1. 用户表 users
-- =====================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(64) NOT NULL COMMENT '微信 openid',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '学业邮箱',
  `password` VARCHAR(255) DEFAULT NULL COMMENT '密码（加密存储）',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `role` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '角色',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `signature` VARCHAR(200) DEFAULT NULL COMMENT '个性签名',
  `email_verified` TINYINT(1) DEFAULT 0 COMMENT '邮箱是否已验证',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `openid` (`openid`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. 邮箱验证码表 email_verification_codes
-- =====================================================
DROP TABLE IF EXISTS `email_verification_codes`;
CREATE TABLE `email_verification_codes` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(128) NOT NULL COMMENT '邮箱地址',
  `code` VARCHAR(10) NOT NULL COMMENT '验证码',
  `purpose` ENUM('REGISTER', 'RESET_PASSWORD', 'BIND_EMAIL') NOT NULL COMMENT '用途',
  `expires_at` TIMESTAMP NOT NULL COMMENT '过期时间',
  `used` TINYINT(1) DEFAULT 0 COMMENT '是否已使用',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_email` (`email`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

-- =====================================================
-- 3. 物品信息表 items
-- =====================================================
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '发布者用户ID',
  `item_type` ENUM('LOST', 'FOUND') NOT NULL COMMENT '物品类型：LOST-寻物，FOUND-拾物',
  `title` VARCHAR(128) NOT NULL COMMENT '标题',
  `category` VARCHAR(32) NOT NULL COMMENT '分类',
  `happen_time` TIMESTAMP NOT NULL COMMENT '丢失/拾到时间',
  `location` VARCHAR(128) NOT NULL COMMENT '地点',
  `feature_desc` TEXT DEFAULT NULL COMMENT '特征描述',
  `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `contact_wechat` VARCHAR(128) DEFAULT NULL COMMENT '联系微信',
  `audit_status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING' COMMENT '审核状态',
  `biz_status` ENUM('ONGOING', 'RESOLVED', 'RETURNED', 'CANCELLED') DEFAULT 'ONGOING' COMMENT '业务状态',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `is_locked` TINYINT(1) DEFAULT 0 COMMENT '是否锁定',
  `visible` TINYINT(1) DEFAULT 1 COMMENT '是否可见（上下架）',
  `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间（审核通过时设置）',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type` (`item_type`),
  KEY `idx_audit` (`audit_status`),
  KEY `idx_biz` (`biz_status`),
  KEY `idx_happen_time` (`happen_time`),
  KEY `idx_published` (`published_at`),
  FULLTEXT KEY `idx_search` (`title`, `location`, `feature_desc`),
  CONSTRAINT `items_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物品信息表';

-- =====================================================
-- 4. 物品图片表 item_images
-- =====================================================
DROP TABLE IF EXISTS `item_images`;
CREATE TABLE `item_images` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `item_id` INT NOT NULL COMMENT '物品ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  CONSTRAINT `item_images_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物品图片表';

-- =====================================================
-- 5. 会话表 conversations
-- =====================================================
DROP TABLE IF EXISTS `conversations`;
CREATE TABLE `conversations` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user1_id` INT NOT NULL COMMENT '参与者1',
  `user2_id` INT NOT NULL COMMENT '参与者2',
  `item_id` INT DEFAULT NULL COMMENT '关联物品ID',
  `last_message_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后消息时间',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pair_item` (`user1_id`, `user2_id`, `item_id`),
  KEY `idx_user1` (`user1_id`),
  KEY `idx_user2` (`user2_id`),
  KEY `idx_last_message` (`last_message_at`),
  KEY `idx_item` (`item_id`),
  CONSTRAINT `conversations_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`),
  CONSTRAINT `conversations_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`),
  CONSTRAINT `conversations_ibfk_3` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- =====================================================
-- 6. 消息表 messages
-- =====================================================
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `conversation_id` INT NOT NULL COMMENT '会话ID',
  `sender_id` INT NOT NULL COMMENT '发送者ID',
  `type` ENUM('TEXT', 'IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '消息类型',
  `content` TEXT DEFAULT NULL COMMENT '消息内容',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '图片URL（图片消息时使用）',
  `status` ENUM('SENT', 'READ') DEFAULT 'SENT' COMMENT '消息状态',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation` (`conversation_id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_created` (`created_at`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- =====================================================
-- 7. 通知表 notifications
-- =====================================================
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '接收者用户ID',
  `type` ENUM('AUDIT', 'SYSTEM', 'MESSAGE') NOT NULL COMMENT '通知类型',
  `title` VARCHAR(128) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `related_id` INT DEFAULT NULL COMMENT '关联ID(如物品ID)',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_read` (`is_read`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- =====================================================
-- 8. 举报表 reports
-- =====================================================
DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `item_id` INT NOT NULL COMMENT '被举报物品ID',
  `reporter_id` INT NOT NULL COMMENT '举报者用户ID',
  `reason_type` ENUM('SCAM', 'AD', 'SENSITIVE', 'OTHER') NOT NULL COMMENT '举报原因类型',
  `reason` TEXT DEFAULT NULL COMMENT '举报详情',
  `status` ENUM('PENDING', 'PROCESSED', 'REJECTED') DEFAULT 'PENDING' COMMENT '处理状态',
  `action` ENUM('REMOVE', 'REJECT', 'IGNORE') DEFAULT NULL COMMENT '处理动作',
  `handler_id` INT DEFAULT NULL COMMENT '处理者管理员ID',
  `handle_result` TEXT DEFAULT NULL COMMENT '处理结果',
  `handled_at` TIMESTAMP NULL DEFAULT NULL COMMENT '处理时间',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_reporter` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_handler` (`handler_id`),
  CONSTRAINT `reports_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `reports_ibfk_2` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `reports_ibfk_3` FOREIGN KEY (`handler_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- =====================================================
-- 9. 审核记录表 audit_logs
-- =====================================================
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `item_id` INT NOT NULL COMMENT '物品ID',
  `auditor_id` INT NOT NULL COMMENT '审核员ID',
  `action` ENUM('APPROVE', 'REJECT') NOT NULL COMMENT '审核动作',
  `reason` TEXT DEFAULT NULL COMMENT '审核原因',
  `audited_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_auditor` (`auditor_id`),
  CONSTRAINT `audit_logs_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `audit_logs_ibfk_2` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- =====================================================
-- 初始化数据: 默认管理员 / 普通用户账号
-- 密码使用 bcrypt 哈希值，此处仅为示例，请在生产环境修改
-- =====================================================
INSERT INTO `users` (`openid`, `email`, `password`, `nickname`, `role`, `email_verified`, `status`)
VALUES (
  'admin_openid',
  'admin@example.com',
  '$2b$10$hQApflksHxWi7MDXeY1z7O8W2OvZhBFqQHyRuqCoE7cCo87WDtFJu',
  '系统管理员',
  'ADMIN',
  1,
  1
),
(
  'user_openid',
  'user@example.com',
  '$2b$10$hQApflksHxWi7MDXeY1z7O8W2OvZhBFqQHyRuqCoE7cCo87WDtFJu',
  '普通用户',
  'USER',
  1,
  1
);

-- =====================================================
-- 初始化完成
-- =====================================================
