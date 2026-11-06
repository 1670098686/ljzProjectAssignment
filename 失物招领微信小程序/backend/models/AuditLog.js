// 审核记录模型
const pool = require('../database/connection');

const AuditLog = {
  async create(data) {
    const { item_id, auditor_id, action, reason } = data;
    const [result] = await pool.execute(
      'INSERT INTO audit_logs (item_id, auditor_id, action, reason) VALUES (?, ?, ?, ?)',
      [item_id, auditor_id, action, reason || null]
    );
    return result.insertId;
  },

  async findByItemId(itemId) {
    const [rows] = await pool.execute(
      `SELECT al.*, u.nickname as auditor_name 
       FROM audit_logs al 
       LEFT JOIN users u ON al.auditor_id = u.id 
       WHERE al.item_id = ? 
       ORDER BY al.audited_at DESC`,
      [itemId]
    );
    return rows;
  }
};

module.exports = AuditLog;
