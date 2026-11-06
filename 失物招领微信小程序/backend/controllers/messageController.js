// 消息控制器
const response = require('../utils/response');
const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const Notification = require('../models/Notification');
const { formatImageUrl } = require('../utils/imageUrl');

// 获取单个会话
exports.getConversation = async (req, res) => {
  try {
    const userId = req.user.id;
    const { conversationId } = req.params;
    
    const conversation = await Conversation.findByIdWithUsers(parseInt(conversationId));
    
    if (!conversation) {
      return response.error(res, 404, '会话不存在');
    }

    if (conversation.user1Id !== userId && conversation.user2Id !== userId) {
      return response.error(res, 403, '无权访问该会话');
    }

    // 格式化用户头像 URL
    const formattedConversation = {
      ...conversation,
      user1: {
        ...conversation.user1,
        avatar: conversation.user1.avatar ? formatImageUrl(conversation.user1.avatar, req) : null
      },
      user2: {
        ...conversation.user2,
        avatar: conversation.user2.avatar ? formatImageUrl(conversation.user2.avatar, req) : null
      }
    };

    response.success(res, formattedConversation);
  } catch (error) {
    response.error(res, 500, `获取会话失败：${error.message}`);
  }
};

// 创建或获取会话
exports.createConversation = async (req, res) => {
  try {
    const userId = req.user.id;
    const { targetUserId, itemId } = req.body;
    
    if (!targetUserId || !itemId) {
      return response.error(res, 400, '缺少必要参数');
    }

    if (targetUserId === userId) {
      return response.error(res, 400, '不能与自己创建会话');
    }

    const conversation = await Conversation.findOrCreate(userId, targetUserId, itemId);

    response.success(res, {
      conversationId: conversation.id
    });
  } catch (error) {
    response.error(res, 500, `创建会话失败：${error.message}`);
  }
};

// 获取会话列表
exports.getConversations = async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, pageSize = 20 } = req.query;
    
    console.log('📞 API getConversations 被调用, userId:', userId); // 调试日志
    
    const result = await Conversation.findByUserId(userId, parseInt(page), parseInt(pageSize));
    
    console.log('📦 Conversation.findByUserId 返回:', JSON.stringify(result, null, 2)); // 调试日志

    // 格式化头像 URL
    const formattedList = result.list.map(conv => ({
      ...conv,
      otherUser: {
        ...conv.otherUser,
        avatar: conv.otherUser.avatar ? formatImageUrl(conv.otherUser.avatar, req) : null
      }
    }));
    
    console.log('✨ 格式化后的数据:', JSON.stringify(formattedList, null, 2)); // 调试日志

    response.page(res, formattedList, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    console.error('❌ getConversations 错误:', error); // 调试日志
    response.error(res, 500, `获取会话列表失败：${error.message}`);
  }
};

// 获取会话消息
exports.getMessages = async (req, res) => {
  try {
    const userId = req.user.id;
    const { conversationId } = req.params;
    const { cursor, limit = 20 } = req.query;
    
    // 校验会话权限
    const conversation = await Conversation.findById(parseInt(conversationId));
    if (!conversation) {
      return response.error(res, 404, '会话不存在');
    }

    if (conversation.user1_id !== userId && conversation.user2_id !== userId) {
      return response.error(res, 403, '无权访问该会话');
    }

    // 自动标记消息为已读
    await Message.markAsRead(parseInt(conversationId), userId);

    const messages = await Message.findByConversationId(
      parseInt(conversationId), 
      cursor, 
      parseInt(limit)
    );

    // 格式化图片 URL
    const formattedMessages = messages.map(msg => ({
      ...msg,
      image_url: msg.image_url ? formatImageUrl(msg.image_url, req) : null
    }));

    response.success(res, formattedMessages);
  } catch (error) {
    response.error(res, 500, `获取消息失败：${error.message}`);
  }
};

// 标记会话消息为已读
exports.markConversationAsRead = async (req, res) => {
  try {
    const userId = req.user.id;
    const { conversationId } = req.params;
    
    // 校验会话权限
    const conversation = await Conversation.findById(parseInt(conversationId));
    if (!conversation) {
      return response.error(res, 404, '会话不存在');
    }

    if (conversation.user1_id !== userId && conversation.user2_id !== userId) {
      return response.error(res, 403, '无权访问该会话');
    }

    await Message.markAsRead(parseInt(conversationId), userId);

    response.success(res, null, '标记成功');
  } catch (error) {
    response.error(res, 500, `标记失败：${error.message}`);
  }
};

// 发送消息
exports.sendMessage = async (req, res) => {
  try {
    const userId = req.user.id;
    const { conversationId } = req.params;
    const { messageType, content, imageUrl } = req.body;
    
    // 校验会话权限
    const conversation = await Conversation.findById(parseInt(conversationId));
    if (!conversation) {
      return response.error(res, 404, '会话不存在');
    }

    if (conversation.user1_id !== userId && conversation.user2_id !== userId) {
      return response.error(res, 403, '无权在该会话发送消息');
    }

    if (!messageType || !['TEXT', 'IMAGE'].includes(messageType)) {
      return response.error(res, 400, '消息类型参数错误');
    }

    if (messageType === 'TEXT' && !content) {
      return response.error(res, 400, '消息内容不能为空');
    }

    if (messageType === 'IMAGE' && !imageUrl) {
      return response.error(res, 400, '图片地址不能为空');
    }

    const messageId = await Message.create(
      parseInt(conversationId),
      userId,
      messageType,
      content,
      imageUrl
    );

    // 更新会话最后消息时间
    await Conversation.updateLastMessage(parseInt(conversationId));

    response.success(res, {
      messageId,
      sentAt: new Date().toISOString()
    }, '发送成功');
  } catch (error) {
    response.error(res, 500, `发送消息失败：${error.message}`);
  }
};

// 获取单个通知详情
exports.getNotificationDetail = async (req, res) => {
  try {
    const userId = req.user.id;
    const { id } = req.params;
    
    const notification = await Notification.findById(id, userId);
    
    if (!notification) {
      return response.error(res, 404, '通知不存在');
    }
    
    response.success(res, notification);
  } catch (error) {
    response.error(res, 500, `获取通知详情失败：${error.message}`);
  }
};

// 获取通知列表
exports.getNotifications = async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, pageSize = 20 } = req.query;
    
    const result = await Notification.findByUserId(userId, parseInt(page), parseInt(pageSize));

    response.page(res, result.list, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取通知失败：${error.message}`);
  }
};

// 标记通知已读
exports.markAsRead = async (req, res) => {
  try {
    const userId = req.user.id;
    const { notificationIds } = req.body;
    
    await Notification.markAsRead(notificationIds, userId);

    response.success(res, null, '标记已读成功');
  } catch (error) {
    response.error(res, 500, `标记已读失败：${error.message}`);
  }
};

// 获取未读通知数量
exports.getUnreadCount = async (req, res) => {
  try {
    const userId = req.user.id;
    const count = await Notification.getUnreadCount(userId);

    response.success(res, { count });
  } catch (error) {
    response.error(res, 500, `获取未读数量失败：${error.message}`);
  }
};

// 获取未读聊天消息数量
exports.getUnreadConversationCount = async (req, res) => {
  try {
    const userId = req.user.id;
    const count = await Message.getTotalUnreadCountByUser(userId);

    response.success(res, { count });
  } catch (error) {
    response.error(res, 500, `获取聊天未读数量失败：${error.message}`);
  }
};
