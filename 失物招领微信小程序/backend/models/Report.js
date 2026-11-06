// 举报模型
const pool = require('../database/connection');

const Report = {
  async create(data) {
    const { item_id, reporter_id, reason_type, reason, evidence_urls } = data;
    const [result] = await pool.execute(
      'INSERT INTO reports (item_id, reporter_id, reason_type, reason) VALUES (?, ?, ?, ?)',
      [item_id, reporter_id, reason_type, reason]
    );
    return result.insertId;
  },

  async findByUserId(userId, page = 1, pageSize = 20) {
    const offset = (page - 1) * pageSize;
    const sql = `
      SELECT r.*, i.title as item_title
      FROM reports r
      LEFT JOIN items i ON r.item_id = i.id
      WHERE r.reporter_id = ?
      ORDER BY r.created_at DESC
      LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}
    `;
    const countSql = 'SELECT COUNT(*) as total FROM reports WHERE reporter_id = ?';
    
    const [rows] = await pool.execute(sql, [userId]);
    const [countRows] = await pool.execute(countSql, [userId]);
    
    return {
      list: rows,
      total: countRows[0].total
    };
  },

  async listAll(page = 1, pageSize = 20, status = null) {
    let sql = `
      SELECT r.*, i.title as item_title, u.nickname as reporter_name
      FROM reports r
      LEFT JOIN items i ON r.item_id = i.id
      LEFT JOIN users u ON r.reporter_id = u.id
      WHERE 1=1
    `;
    let countSql = 'SELECT COUNT(*) as total FROM reports r WHERE 1=1';
    const values = [];
    const countValues = [];
    
    if (status) {
      sql += ' AND r.status = ?';
      countSql += ' AND status = ?';
      values.push(status);
      countValues.push(status);
    }
    
    const offset = (page - 1) * pageSize;
    sql += ` ORDER BY r.created_at DESC LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}`;
    
    const [rows] = await pool.execute(sql, values);
    const [countRows] = await pool.execute(countSql, countValues);
    
    return {
      list: rows,
      total: countRows[0].total
    };
  },

  async findById(id) {
    const [rows] = await pool.execute(
      'SELECT * FROM reports WHERE id = ?',
      [id]
    );
    return rows[0] || null;
  },

  async updateStatus(id, status, action = null, note = null, handlerId = null) {
    await pool.execute(
      'UPDATE reports SET status = ?, action = ?, handle_result = ?, handler_id = ?, handled_at = NOW() WHERE id = ?',
      [status, action, note, handlerId, id]
    );
  }
};

module.exports = Report;
