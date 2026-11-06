// 物品图片模型
const pool = require('../database/connection');

const ItemImage = {
  async findByItemId(itemId) {
    const [rows] = await pool.execute(
      'SELECT id, item_id, image_url, sort, created_at FROM item_images WHERE item_id = ? ORDER BY sort ASC',
      [itemId]
    );
    return rows;
  },

  async createBatch(itemId, imageUrls) {
    if (!imageUrls || imageUrls.length === 0) return;
    
    const values = imageUrls.map((url, index) => [itemId, url, index]);
    await pool.query(
      'INSERT INTO item_images (item_id, image_url, sort) VALUES ?',
      [values]
    );
  },

  async deleteByItemId(itemId) {
    await pool.execute(
      'DELETE FROM item_images WHERE item_id = ?',
      [itemId]
    );
  }
};

module.exports = ItemImage;
