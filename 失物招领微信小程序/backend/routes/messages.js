// 消息路由
const express = require('express');
const router = express.Router();
const messageController = require('../controllers/messageController');
const auth = require('../middlewares/auth');

// 创建会话
router.post('/conversations', auth.verify, messageController.createConversation);

// 获取会话列表
router.get('/conversations', auth.verify, messageController.getConversations);

// 获取未读聊天数量 (需要放在动态路由之前)
router.get('/conversations/unread-count', auth.verify, messageController.getUnreadConversationCount);

// 获取单个会话
router.get('/conversations/:conversationId', auth.verify, messageController.getConversation);

// 获取会话消息
router.get('/conversations/:conversationId/messages', auth.verify, messageController.getMessages);

// 标记会话消息为已读
router.patch('/conversations/:conversationId/read', auth.verify, messageController.markConversationAsRead);

// 发送消息
router.post('/conversations/:conversationId/messages', auth.verify, messageController.sendMessage);

// 获取未读通知数量 (需要放在动态路由之前)
router.get('/notifications/unread-count', auth.verify, messageController.getUnreadCount);

// 获取通知列表
router.get('/notifications', auth.verify, messageController.getNotifications);

// 获取单个通知详情
router.get('/notifications/:id', auth.verify, messageController.getNotificationDetail);

// 标记通知已读
router.patch('/notifications/read', auth.verify, messageController.markAsRead);

module.exports = router;
