// 物品信息模型
const pool = require('../database/connection');

const Item = {
  async findById(id) {
    const [rows] = await pool.execute(
      `SELECT i.id, i.user_id, i.item_type, i.title, i.category, i.happen_time, i.location, 
       i.feature_desc, i.contact_name, i.contact_phone, i.contact_wechat,
       i.audit_status, i.biz_status, i.view_count, i.is_locked, i.visible, i.published_at,
       i.created_at, i.updated_at
       FROM items i WHERE i.id = ?`,
      [id]
    );
    return rows[0] || null;
  },

  async create(data) {
    const {
      user_id,
      item_type,
      title,
      category,
      happen_time,
      location,
      feature_desc,
      contact_name,
      contact_phone,
      contact_wechat
    } = data;
    const [result] = await pool.execute(
      `INSERT INTO items (user_id, item_type, title, category, happen_time, location, 
       feature_desc, contact_name, contact_phone, contact_wechat) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        user_id,
        item_type,
        title,
        category,
        happen_time,
        location,
        feature_desc,
        contact_name,
        contact_phone || null,
        contact_wechat || null
      ]
    );
    return result.insertId;
  },

  async update(id, data) {
    const fields = [];
    const values = [];
    const hasAuditStatus = data.hasOwnProperty('audit_status');
    
    Object.keys(data).forEach(key => {
      if (data[key] !== undefined) {
        fields.push(`${key} = ?`);
        values.push(data[key]);
      }
    });
    
    if (fields.length === 0) return;
    
    // 只有在没有明确设置 audit_status 的情况下，才会将编辑后的物品重新进入待审核
    if (!hasAuditStatus) {
      fields.push('audit_status = ?');
      values.push('PENDING');
    }
    
    values.push(id);
    await pool.execute(
      `UPDATE items SET ${fields.join(', ')} WHERE id = ?`,
      values
    );
  },

  async updateAuditStatus(id, auditStatus, auditedAt = null) {
    const sql = auditedAt 
      ? 'UPDATE items SET audit_status = ?, published_at = ? WHERE id = ?'
      : 'UPDATE items SET audit_status = ? WHERE id = ?';
    const values = auditedAt ? [auditStatus, auditedAt, id] : [auditStatus, id];
    await pool.execute(sql, values);
  },

  async updateBizStatus(id, bizStatus) {
    await pool.execute(
      'UPDATE items SET biz_status = ? WHERE id = ?',
      [bizStatus, id]
    );
  },

  async incrementViewCount(id) {
    await pool.execute(
      'UPDATE items SET view_count = view_count + 1 WHERE id = ?',
      [id]
    );
  },

  async list(page = 1, pageSize = 10, filters = {}) {
    let sql = `SELECT i.id, i.user_id, i.item_type, i.title, i.category, i.happen_time, 
               i.location, i.audit_status, i.biz_status, i.view_count, i.published_at
               FROM items i WHERE 1=1`;
    let countSql = 'SELECT COUNT(*) as total FROM items WHERE 1=1';
    const values = [];
    const countValues = [];

    if (Array.isArray(filters.auditStatuses) && filters.auditStatuses.length > 0) {
      const placeholders = filters.auditStatuses.map(() => '?').join(', ');
      sql += ` AND i.audit_status IN (${placeholders})`;
      countSql += ` AND audit_status IN (${placeholders})`;
      values.push(...filters.auditStatuses);
      countValues.push(...filters.auditStatuses);
    } else if (!filters.includePending && !filters.includeRejected) {
      // 默认只返回审核通过且未下架的信息
      sql += ' AND i.audit_status = ? AND i.visible = 1';
      countSql += ' AND audit_status = ? AND visible = 1';
      values.push('APPROVED');
      countValues.push('APPROVED');
    }

    if (filters.itemType) {
      sql += ' AND i.item_type = ?';
      countSql += ' AND item_type = ?';
      values.push(filters.itemType);
      countValues.push(filters.itemType);
    }

    if (filters.userId) {
      sql += ' AND i.user_id = ?';
      countSql += ' AND user_id = ?';
      values.push(filters.userId);
      countValues.push(filters.userId);
    }

    if (filters.auditStatus) {
      sql += ' AND i.audit_status = ?';
      countSql += ' AND audit_status = ?';
      values.push(filters.auditStatus);
      countValues.push(filters.auditStatus);
    }

    if (filters.bizStatus) {
      sql += ' AND i.biz_status = ?';
      countSql += ' AND biz_status = ?';
      values.push(filters.bizStatus);
      countValues.push(filters.bizStatus);
    } else if (Array.isArray(filters.bizStatuses) && filters.bizStatuses.length > 0) {
      const placeholders = filters.bizStatuses.map(() => '?').join(', ');
      sql += ` AND i.biz_status IN (${placeholders})`;
      countSql += ` AND biz_status IN (${placeholders})`;
      values.push(...filters.bizStatuses);
      countValues.push(...filters.bizStatuses);
    }

    if (filters.keyword) {
      sql += ' AND (i.title LIKE ? OR i.location LIKE ? OR i.feature_desc LIKE ?)';
      countSql += ' AND (title LIKE ? OR location LIKE ? OR feature_desc LIKE ?)';
      const likeKeyword = `%${filters.keyword}%`;
      values.push(likeKeyword, likeKeyword, likeKeyword);
      countValues.push(likeKeyword, likeKeyword, likeKeyword);
    }

    if (filters.category) {
      sql += ' AND i.category = ?';
      countSql += ' AND category = ?';
      values.push(filters.category);
      countValues.push(filters.category);
    }

    if (filters.location) {
      sql += ' AND i.location LIKE ?';
      countSql += ' AND location LIKE ?';
      const likeLocation = `%${filters.location}%`;
      values.push(likeLocation);
      countValues.push(likeLocation);
    }

    if (filters.startTime) {
      sql += ' AND i.happen_time >= ?';
      countSql += ' AND happen_time >= ?';
      values.push(filters.startTime);
      countValues.push(filters.startTime);
    }

    if (filters.endTime) {
      sql += ' AND i.happen_time <= ?';
      countSql += ' AND happen_time <= ?';
      values.push(filters.endTime);
      countValues.push(filters.endTime);
    }

    // 排序
    if (filters.sortBy === 'happenTime') {
      sql += ' ORDER BY i.happen_time DESC';
    } else {
      sql += ' ORDER BY i.created_at DESC';
    }

    const offset = (page - 1) * pageSize;
    sql += ` LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}`;

    const [rows] = await pool.query(sql, values);
    const [countRows] = await pool.query(countSql, countValues);
    
    return {
      list: rows,
      total: countRows[0].total
    };
  },

  async listPending(page = 1, pageSize = 10, itemType = null) {
    let sql = `SELECT i.id, i.user_id, i.item_type, i.title, i.category, i.happen_time, 
               i.location, i.audit_status, i.biz_status, i.created_at
               FROM items i WHERE i.audit_status = 'PENDING'`;
    let countSql = "SELECT COUNT(*) as total FROM items WHERE audit_status = 'PENDING'";
    const values = [];

    if (itemType) {
      sql += ' AND i.item_type = ?';
      countSql += ' AND item_type = ?';
      values.push(itemType);
    }

    const offset = (page - 1) * pageSize;
    sql += ` ORDER BY i.created_at ASC LIMIT ${Number(pageSize)} OFFSET ${Number(offset)}`;

    const [rows] = await pool.execute(sql, values);
    const [countRows] = await pool.execute(countSql, values.slice(0, -2));
    
    return {
      list: rows,
      total: countRows[0].total
    };
  }
};

module.exports = Item;
