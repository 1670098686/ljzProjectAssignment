-- 清空聊天数据的脚本
-- 此脚本会删除所有会话和消息数据，但保留其他表的内容

USE `lost_and_found`;

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空消息表
TRUNCATE TABLE `messages`;

-- 清空会话表
TRUNCATE TABLE `conversations`;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

SELECT '聊天数据已清空' AS message;
