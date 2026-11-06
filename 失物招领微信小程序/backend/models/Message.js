// 消息模型
const pool = require('../database/connection');

const Message = {
  async create(conversationId, senderId, messageType, content, imageUrl = null) {
    const [result] = await pool.execute(
      'INSERT INTO messages (conversation_id, sender_id, type, content, image_url, status) VALUES (?, ?, ?, ?, ?, ?)',
      [conversationId, senderId, messageType, content || '', imageUrl, 'SENT']
    );
    return result.insertId;
  },

  async findByConversationId(conversationId, cursor = null, limit = 20) {
    let sql = 'SELECT * FROM messages WHERE conversation_id = ?';
    const values = [conversationId];
    
    if (cursor) {
      sql += ' AND id < ?';
      values.push(parseInt(cursor));
    }
    
    sql += ` ORDER BY created_at DESC LIMIT ${parseInt(limit)}`;

    const [rows] = await pool.execute(sql, values);
    return rows.reverse(); // 按时间正序返回
  },

  async findLastByConversationId(conversationId) {
    const sql = 'SELECT * FROM messages WHERE conversation_id = ? ORDER BY created_at DESC LIMIT 1';
    const [rows] = await pool.execute(sql, [conversationId]);
    return rows[0] || null;
  },

  async markAsRead(conversationId, userId) {
    await pool.execute(
      "UPDATE messages SET status = 'READ' WHERE conversation_id = ? AND sender_id != ? AND (status != 'READ' OR status IS NULL)",
      [conversationId, userId]
    );
  },

  async getUnreadCount(conversationId, userId) {
    const [rows] = await pool.execute(
      "SELECT COUNT(*) as count FROM messages WHERE conversation_id = ? AND sender_id != ? AND (status != 'READ' OR status IS NULL)",
      [conversationId, userId]
    );
    return rows[0].count;
  },

  async getTotalUnreadCountByUser(userId) {
    const [rows] = await pool.execute(
      `SELECT COUNT(*) as count
       FROM messages m
       INNER JOIN conversations c ON m.conversation_id = c.id
       WHERE (c.user1_id = ? OR c.user2_id = ?)
         AND m.sender_id != ?
         AND (m.status != 'READ' OR m.status IS NULL)`,
      [userId, userId, userId]
    );
    return rows[0].count;
  },

  async markOldMessagesAsSent(conversationId) {
    await pool.execute(
      "UPDATE messages SET status = 'SENT' WHERE conversation_id = ? AND status IS NULL",
      [conversationId]
    );
  }
};

module.exports = Message;
