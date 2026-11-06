require('dotenv').config();
const mysql = require('mysql2/promise');

(async () => {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
  });

  try {
    console.log('=== 数据清理 ===\n');

    // 1. 找出并删除标题含"测试"的物品
    const [testItems] = await conn.query(
      "SELECT id, title, user_id, audit_status FROM items WHERE title LIKE '%测试%'"
    );

    console.log(`找到 ${testItems.length} 条标题含'测试'的物品:`);
    testItems.forEach(item => {
      console.log(`  - #${item.id}: ${item.title} (发布者:${item.user_id})`);
    });

    let deletedItemCount = 0;
    let deletedImageCount = 0;

    if (testItems.length > 0) {
      const itemIds = testItems.map(i => i.id);
      const placeholders = itemIds.map(() => '?').join(',');

      // 先删除相关的物品图片
      const [imgResult] = await conn.query(
        `DELETE FROM item_images WHERE item_id IN (${placeholders})`,
        itemIds
      );
      deletedImageCount = imgResult.affectedRows;

      // 删除相关的举报记录
      await conn.query(
        `DELETE FROM reports WHERE item_id IN (${placeholders})`,
        itemIds
      );

      // 删除相关的审核日志
      await conn.query(
        `DELETE FROM audit_logs WHERE item_id IN (${placeholders})`,
        itemIds
      );

      // 删除相关的会话和消息
      const [convs] = await conn.query(
        `SELECT id FROM conversations WHERE item_id IN (${placeholders})`,
        itemIds
      );
      if (convs.length > 0) {
        const convIds = convs.map(c => c.id);
        const convPlaceholders = convIds.map(() => '?').join(',');
        await conn.query(
          `DELETE FROM messages WHERE conversation_id IN (${convPlaceholders})`,
          convIds
        );
        await conn.query(
          `DELETE FROM conversations WHERE id IN (${convPlaceholders})`,
          convIds
        );
      }

      // 最后删除物品
      const [itemResult] = await conn.query(
        `DELETE FROM items WHERE id IN (${placeholders})`,
        itemIds
      );
      deletedItemCount = itemResult.affectedRows;
    }

    console.log(`\n✓ 已删除 ${deletedItemCount} 条物品, ${deletedImageCount} 张物品图片`);

    // 2. 清空 notifications 表
    const [notifResult] = await conn.query('SELECT COUNT(*) as cnt FROM notifications');
    const oldCount = notifResult[0].cnt;
    await conn.query('DELETE FROM notifications');

    // 重置自增
    await conn.query('ALTER TABLE notifications AUTO_INCREMENT = 1');

    console.log(`✓ 已清空系统通知 (${oldCount} 条)`);

    // 3. 显示当前剩余数据
    const [itemTotal] = await conn.query('SELECT COUNT(*) as cnt FROM items');
    const [notifTotal] = await conn.query('SELECT COUNT(*) as cnt FROM notifications');

    console.log('\n=== 当前数据状态 ===');
    console.log(`  物品总数: ${itemTotal[0].cnt}`);
    console.log(`  通知总数: ${notifTotal[0].cnt}`);

    const [remaining] = await conn.query('SELECT id, title, item_type, user_id FROM items LIMIT 10');
    if (remaining.length > 0) {
      console.log('\n  剩余物品列表:');
      remaining.forEach(item => {
        console.log(`    #${item.id}: ${item.title}`);
      });
    }

    console.log('\n✓ 清理完成\n');
  } catch (err) {
    console.error('清理失败:', err.message);
    process.exit(1);
  } finally {
    await conn.end();
  }
})();
