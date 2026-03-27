-- 初始化数据库表结构
-- 个人收支记账APP - 数据库初始化脚本

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 创建分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(50) DEFAULT NULL,
  `type` INT NOT NULL COMMENT '1=收入, 2=支出',
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_category_user` (`user_id`),
  INDEX `idx_category_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 创建交易记录表
CREATE TABLE IF NOT EXISTS `transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` INT NOT NULL COMMENT '1=收入, 2=支出',
  `category_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `transaction_date` DATE NOT NULL,
  `remark` VARCHAR(100) DEFAULT NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_transactions_user_date` (`user_id`, `transaction_date`),
  INDEX `idx_transactions_user_type_category` (`user_id`, `type`, `category_id`),
  INDEX `idx_user_recent` (`user_id`, `transaction_date DESC`),
  CONSTRAINT `fk_transaction_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表';

-- 创建预算表
CREATE TABLE IF NOT EXISTS `budget` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `category_id` BIGINT NOT NULL,
  `monthly_budget` DECIMAL(10,2) NOT NULL,
  `year` INT NOT NULL,
  `month` INT NOT NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_user_category_month` (`user_id`, `category_id`, `year`, `month`),
  INDEX `idx_budget_user_month` (`user_id`, `year`, `month`),
  CONSTRAINT `fk_budget_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';

-- 创建储蓄目标表
CREATE TABLE IF NOT EXISTS `saving_goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `target_amount` DECIMAL(10,2) NOT NULL,
  `current_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `deadline` DATE NOT NULL,
  `description` TEXT,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_saving_goal_user` (`user_id`),
  INDEX `idx_saving_goal_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='储蓄目标表';

-- 创建用户配置表
CREATE TABLE IF NOT EXISTS `user_profile` (
  `user_id` BIGINT NOT NULL,
  `nickname` VARCHAR(30) NOT NULL,
  `remind_time` VARCHAR(10) DEFAULT NULL,
  `backup_status` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户配置表';

-- 插入默认分类数据
INSERT INTO `category` (`name`, `icon`, `type`, `user_id`, `create_time`, `update_time`) VALUES
('工资', 'work', 1, 1, NOW(), NOW()),
('奖金', 'bonus', 1, 1, NOW(), NOW()),
('投资收益', 'investment', 1, 1, NOW(), NOW()),
('其他收入', 'other_income', 1, 1, NOW(), NOW()),

('餐饮', 'restaurant', 2, 1, NOW(), NOW()),
('交通', 'transport', 2, 1, NOW(), NOW()),
('购物', 'shopping', 2, 1, NOW(), NOW()),
('娱乐', 'entertainment', 2, 1, NOW(), NOW()),
('医疗', 'medical', 2, 1, NOW(), NOW()),
('教育', 'education', 2, 1, NOW(), NOW()),
('住房', 'housing', 2, 1, NOW(), NOW()),
('其他支出', 'other_expense', 2, 1, NOW(), NOW());

-- 插入示例用户配置
INSERT INTO `user_profile` (`user_id`, `nickname`, `remind_time`, `create_time`, `update_time`) VALUES
(1, '示例用户', '21:00', NOW(), NOW());