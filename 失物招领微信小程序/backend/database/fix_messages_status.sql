-- 更新现有的消息，将NULL的status设置为'SENT'
-- 这样可以确保未读计数正常工作

UPDATE messages 
SET status = 'SENT' 
WHERE status IS NULL;

-- 验证更新结果
SELECT 
    id, 
    conversation_id, 
    sender_id, 
    type, 
    status, 
    created_at 
FROM messages 
ORDER BY created_at DESC 
LIMIT 20;
