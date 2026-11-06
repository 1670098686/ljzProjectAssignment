-- 数据库迁移脚本：修复会话表唯一键约束
-- 执行日期：2024-06-06
-- 说明：将原来的 uk_user1_user2 唯一键改为 uk_user1_user2_item，支持同一对用户针对不同物品创建独立会话

USE lost_and_found;

-- 删除旧的唯一键约束
ALTER TABLE `conversations` DROP INDEX `uk_user1_user2`;

-- 添加新的唯一键约束（包含用户1、用户2和物品）
ALTER TABLE `conversations` ADD UNIQUE KEY `uk_user1_user2_item` (`user1_id`, `user2_id`, `item_id`);

-- 添加物品索引
ALTER TABLE `conversations` ADD INDEX `idx_item_id` (`item_id`);

SELECT '数据库迁移成功！';
