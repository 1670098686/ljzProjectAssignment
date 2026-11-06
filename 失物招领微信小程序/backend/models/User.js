// 用户模型
const pool = require('../database/connection');

const User = {
  async findById(id) {
    const [rows] = await pool.execute(
      'SELECT id, openid, unionid, email, nickname, avatar_url, role, phone, signature, email_verified, status, created_at, updated_at FROM users WHERE id = ?',
      [id]
    );
    return rows[0] || null;
  },

  async findByOpenid(openid) {
    const [rows] = await pool.execute(
      'SELECT id, openid, unionid, email, password, nickname, avatar_url, role, phone, signature, email_verified, status, created_at, updated_at FROM users WHERE openid = ?',
      [openid]
    );
    return rows[0] || null;
  },

  async findByEmail(email) {
    const [rows] = await pool.execute(
      'SELECT id, openid, unionid, email, password, nickname, avatar_url, role, phone, signature, email_verified, status, created_at, updated_at FROM users WHERE email = ?',
      [email]
    );
    return rows[0] || null;
  },

  async create(data) {
    const { openid, unionid, email, password, nickname, avatar_url, role } = data;
    const [result] = await pool.execute(
      'INSERT INTO users (openid, unionid, email, password, nickname, avatar_url, role) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [
        openid, 
        unionid, 
        email, 
        password, 
        nickname, 
        avatar_url || null,  // 如果avatar_url是undefined，使用null
        role || 'USER'
      ]
    );
    return result.insertId;
  },

  async update(id, data) {
    const fields = [];
    const values = [];
    
    Object.keys(data).forEach(key => {
      if (data[key] !== undefined) {
        fields.push(`${key} = ?`);
        values.push(data[key]);
      }
    });
    
    if (fields.length === 0) return;
    
    values.push(id);
    await pool.execute(
      `UPDATE users SET ${fields.join(', ')} WHERE id = ?`,
      values
    );
  },

  async updateStatus(id, status) {
    await pool.execute(
      'UPDATE users SET status = ? WHERE id = ?',
      [status, id]
    );
  },

  async list(page = 1, pageSize = 10, filters = {}) {
    let sql = 'SELECT id, openid, email, nickname, avatar_url, role, phone, email_verified, status, created_at FROM users WHERE 1=1';
    let countSql = 'SELECT COUNT(*) as total FROM users WHERE 1=1';
    const values = [];
    const countValues = [];

    if (filters.role) {
      sql += ' AND role = ?';
      countSql += ' AND role = ?';
      values.push(filters.role);
      countValues.push(filters.role);
    }

    if (filters.status !== undefined) {
      sql += ' AND status = ?';
      countSql += ' AND status = ?';
      values.push(filters.status);
      countValues.push(filters.status);
    }

    const offset = (page - 1) * pageSize;
    sql += ` ORDER BY created_at DESC LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}`;

    const [rows] = await pool.execute(sql, values);
    const [countRows] = await pool.execute(countSql, countValues);
    
    return {
      list: rows,
      total: countRows[0].total
    };
  }
};

module.exports = User;
