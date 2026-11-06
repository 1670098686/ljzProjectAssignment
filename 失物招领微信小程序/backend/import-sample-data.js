require('dotenv').config();
const fs = require('fs');
const mysql = require('mysql2/promise');

async function importData() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    multipleStatements: true
  });

  try {
    console.log('开始导入 sample_data.sql...');
    const sampleSql = fs.readFileSync('./sql/sample_data.sql', 'utf8');
    await conn.query(sampleSql);
    console.log('✓ 物品数据导入成功');

    console.log('开始导入 item_images_data.sql...');
    const imagesSql = fs.readFileSync('./sql/item_images_data.sql', 'utf8');
    await conn.query(imagesSql);
    console.log('✓ 物品图片数据导入成功');

    const [items] = await conn.query('SELECT COUNT(*) as cnt FROM items');
    const [images] = await conn.query('SELECT COUNT(*) as cnt FROM item_images');
    console.log(`\n当前物品总数: ${items[0].cnt}`);
    console.log(`当前物品图片总数: ${images[0].cnt}`);

    const [itemsList] = await conn.query('SELECT id, user_id, item_type, title, contact_name, contact_phone, contact_wechat FROM items');
    console.log('\n物品列表:');
    console.table(itemsList);
  } catch (err) {
    console.error('导入失败:', err.message);
    process.exit(1);
  } finally {
    await conn.end();
  }
}

importData();
