// 会话模型
const pool = require('../database/connection');
const Message = require('./Message');

const Conversation = {
  async findOrCreate(user1Id, user2Id, itemId) {
    // 确保 user1_id < user2_id 以避免重复会话
    const [uid1, uid2] = user1Id < user2Id ? [user1Id, user2Id] : [user2Id, user1Id];
    
    // 查找现有会话（需要同时匹配用户和物品）
    const [rows] = await pool.execute(
      'SELECT id, item_id FROM conversations WHERE user1_id = ? AND user2_id = ? AND item_id = ?',
      [uid1, uid2, itemId]
    );
    
    if (rows.length > 0) {
      return rows[0];
    }
    
    // 创建新会话
    const [result] = await pool.execute(
      'INSERT INTO conversations (user1_id, user2_id, item_id) VALUES (?, ?, ?)',
      [uid1, uid2, itemId]
    );
    
    return { id: result.insertId, item_id: itemId };
  },

  async findByUserId(userId, page = 1, pageSize = 20) {
    const offset = (page - 1) * pageSize;
    const sql = `
      SELECT c.id, c.item_id, c.user1_id, c.user2_id, c.last_message_at, c.updated_at,
             i.title as item_title, i.item_type,
             u1.nickname as user1_nickname, u1.avatar_url as user1_avatar_url,
             u2.nickname as user2_nickname, u2.avatar_url as user2_avatar_url
      FROM conversations c
      LEFT JOIN items i ON c.item_id = i.id
      LEFT JOIN users u1 ON c.user1_id = u1.id
      LEFT JOIN users u2 ON c.user2_id = u2.id
      WHERE c.user1_id = ? OR c.user2_id = ?
      ORDER BY c.last_message_at DESC, c.updated_at DESC
      LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}
    `;
    const countSql = `
      SELECT COUNT(*) as total FROM conversations
      WHERE user1_id = ? OR user2_id = ?
    `;

    const [rows] = await pool.execute(sql, [userId, userId]);
    const [countRows] = await pool.execute(countSql, [userId, userId]);
    
    // 处理数据，确定对方用户，并获取每个会话的最后消息和未读数量
    const list = await Promise.all(rows.map(async row => {
      // 暂时禁用，看看是不是这里导致的问题
      // await Message.markOldMessagesAsSent(row.id);
      
      const isUser1 = row.user1_id === userId;
      const otherUser = {
        id: isUser1 ? row.user2_id : row.user1_id,
        nickname: isUser1 ? row.user2_nickname : row.user1_nickname,
        avatar: isUser1 ? row.user2_avatar_url : row.user1_avatar_url
      };
      
      // 获取最后一条消息
      const lastMessage = await Message.findLastByConversationId(row.id);
      // 获取未读消息数量
      const unreadCount = await Message.getUnreadCount(row.id, userId);
      
      console.log(`DEBUG: 会话 ${row.id}, 最后消息:`, lastMessage);
      console.log(`DEBUG: 会话 ${row.id}, 未读计数:`, unreadCount);
      
      return {
        id: row.id,
        itemId: row.item_id,
        itemTitle: row.item_title,
        itemType: row.item_type,
        lastMessageAt: row.last_message_at,
        otherUser: otherUser,
        lastMessage: lastMessage ? (lastMessage.type === 'IMAGE' ? '[图片]' : lastMessage.content) : '暂无消息',
        unreadCount: unreadCount
      };
    }));
    
    return {
      list: list,
      total: countRows[0].total
    };
  },

  async updateLastMessage(conversationId) {
    await pool.execute(
      'UPDATE conversations SET last_message_at = NOW(), updated_at = NOW() WHERE id = ?',
      [conversationId]
    );
  },

  async findById(conversationId) {
    const [rows] = await pool.execute(
      'SELECT * FROM conversations WHERE id = ?',
      [conversationId]
    );
    return rows[0] || null;
  },

  async findByIdWithUsers(conversationId) {
    const [rows] = await pool.execute(
      `SELECT c.*, 
              u1.id as user1_id, u1.nickname as user1_nickname, u1.avatar_url as user1_avatar_url,
              u2.id as user2_id, u2.nickname as user2_nickname, u2.avatar_url as user2_avatar_url
       FROM conversations c
       LEFT JOIN users u1 ON c.user1_id = u1.id
       LEFT JOIN users u2 ON c.user2_id = u2.id
       WHERE c.id = ?`,
      [conversationId]
    );
    
    if (rows.length === 0) return null;
    
    const row = rows[0];
    return {
      id: row.id,
      itemId: row.item_id,
      user1Id: row.user1_id,
      user2Id: row.user2_id,
      lastMessageAt: row.last_message_at,
      user1: {
        id: row.user1_id,
        nickname: row.user1_nickname,
        avatar: row.user1_avatar_url
      },
      user2: {
        id: row.user2_id,
        nickname: row.user2_nickname,
        avatar: row.user2_avatar_url
      }
    };
  }
};

module.exports = Conversation;
