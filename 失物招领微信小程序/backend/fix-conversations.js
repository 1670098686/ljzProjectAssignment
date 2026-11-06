require('dotenv').config();
const mysql = require('mysql2/promise');

async function fixConversations() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
  });

  try {
    console.log('检查 conversations 表的唯一约束...');

    const [constraints] = await conn.query(`
      SELECT CONSTRAINT_NAME 
      FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
      WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'conversations'
      AND CONSTRAINT_NAME LIKE 'uk%'
    `);

    console.log('当前约束:', constraints.map(c => c.CONSTRAINT_NAME));

    if (constraints.length > 0) {
      const name = constraints[0].CONSTRAINT_NAME;
      console.log(`删除旧约束:`, name);
      await conn.execute(`ALTER TABLE conversations DROP CONSTRAINT ${name}`);
      console.log('✓ 已删除旧的唯一约束');
    }

    console.log('添加新的唯一约束 (user1_id, user2_id, item_id)...');
    await conn.execute(`
      ALTER TABLE conversations 
      ADD CONSTRAINT uk_user_pair_item 
      UNIQUE (user1_id, user2_id, item_id)
    `);
    console.log('✓ 新的唯一约束已添加 - 现在同一对用户与不同物品可以创建不同的对话');

    const [rows] = await conn.query(`
      SELECT COUNT(*) as cnt FROM conversations
    `);
    console.log(`\n当前对话数量: ${rows[0].cnt}`);

    console.log('\n✓ 修复完成！');
  } catch (err) {
    console.error('修复失败:', err.message);
    process.exit(1);
  } finally {
    await conn.end();
  }
}

fixConversations();
