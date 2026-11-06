// 通知模型
const pool = require('../database/connection');

const Notification = {
  async create(userId, type, title, content, relatedId = null) {
    const [result] = await pool.execute(
      'INSERT INTO notifications (user_id, type, title, content, related_id) VALUES (?, ?, ?, ?, ?)',
      [userId, type, title, content, relatedId]
    );
    return result.insertId;
  },

  // 批量创建通知（发送系统通知）
  async createBatch(userIds, type, title, content, relatedId = null) {
    if (!userIds || userIds.length === 0) {
      return 0;
    }
    
    const placeholders = userIds.map(() => '(?, ?, ?, ?, ?)').join(',');
    const values = userIds.flatMap(userId => [userId, type, title, content, relatedId]);
    
    const [result] = await pool.execute(
      `INSERT INTO notifications (user_id, type, title, content, related_id) VALUES ${placeholders}`,
      values
    );
    return result.affectedRows;
  },

  // 获取所有用户ID（用于发送全员通知）
  async getAllUserIds(excludeUserId = null) {
    let sql = 'SELECT id FROM users WHERE status = 1';
    const values = [];
    
    if (excludeUserId) {
      sql += ' AND id != ?';
      values.push(excludeUserId);
    }
    
    const [rows] = await pool.execute(sql, values);
    return rows.map(row => row.id);
  },

  async findByUserId(userId, page = 1, pageSize = 20) {
    const offset = (page - 1) * pageSize;
    const sql = `SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}`;
    const countSql = 'SELECT COUNT(*) as total FROM notifications WHERE user_id = ?';

    const [rows] = await pool.execute(sql, [userId]);
    const [countRows] = await pool.execute(countSql, [userId]);
    
    const list = rows.map(row => ({
      id: row.id,
      userId: row.user_id,
      type: row.type,
      title: row.title,
      content: row.content,
      relatedId: row.related_id,
      isRead: row.is_read === 1,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));
    
    return {
      list: list,
      total: countRows[0].total
    };
  },

  async markAsRead(notificationIds = null, userId = null) {
    if (notificationIds && notificationIds.length > 0) {
      const placeholders = notificationIds.map(() => '?').join(',');
      await pool.execute(
        `UPDATE notifications SET is_read = 1 WHERE id IN (${placeholders}) AND user_id = ?`,
        [...notificationIds, userId]
      );
    } else {
      await pool.execute(
        'UPDATE notifications SET is_read = 1 WHERE user_id = ?',
        [userId]
      );
    }
  },

  async getUnreadCount(userId) {
    const [rows] = await pool.execute(
      'SELECT COUNT(*) as count FROM notifications WHERE user_id = ? AND is_read = 0',
      [userId]
    );
    return rows[0].count;
  },

  async findById(id, userId) {
    const [rows] = await pool.execute(
      'SELECT * FROM notifications WHERE id = ? AND user_id = ?',
      [id, userId]
    );
    
    if (rows.length === 0) return null;
    
    return {
      id: rows[0].id,
      userId: rows[0].user_id,
      type: rows[0].type,
      title: rows[0].title,
      content: rows[0].content,
      relatedId: rows[0].related_id,
      isRead: rows[0].is_read === 1,
      createdAt: rows[0].created_at,
      updatedAt: rows[0].updated_at
    };
  }
};

module.exports = Notification;
