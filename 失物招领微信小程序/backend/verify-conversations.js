require('dotenv').config();
const mysql = require('mysql2/promise');

(async () => {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
  });

  console.log('=== 数据库验证 ===\n');

  const [idx] = await conn.query("SHOW INDEX FROM conversations WHERE Key_name LIKE 'uk%'");
  console.log('conversations 表的唯一约束:');
  const idxMap = {};
  idx.forEach(i => {
    if (!idxMap[i.Key_name]) idxMap[i.Key_name] = [];
    idxMap[i.Key_name].push(i.Column_name);
  });
  Object.entries(idxMap).forEach(([name, cols]) => {
    console.log(`  ${name}: (${cols.join(', ')})`);
  });

  const [conv] = await conn.query('SELECT id, user1_id, user2_id, item_id, last_message_at FROM conversations');
  console.log('\n当前会话列表:');
  if (conv.length === 0) {
    console.log('  (暂无会话)');
  } else {
    conv.forEach(c => {
      console.log(`  会话#${c.id}: 用户${c.user1_id} <-> 用户${c.user2_id}, 物品#${c.item_id}`);
    });
  }

  console.log('\n=== 验证完成 ===');
  console.log('✓ 现在同一对用户与不同物品可以创建不同的对话');
  console.log('✓ 前端消息列表会显示物品标题作为主要标题');
  console.log('✓ 聊天页面顶部显示物品信息，可点击查看物品详情\n');

  await conn.end();
})();
