// 清空聊天数据的脚本（直接执行，不需要确认）
const pool = require('./connection');

async function clearChatData() {
  console.log('开始清空聊天数据...');

  const connection = await pool.getConnection();

  try {
    // 禁用外键检查
    await connection.execute('SET FOREIGN_KEY_CHECKS = 0');
    console.log('[OK] 已禁用外键检查');

    // 清空消息表
    const [messagesResult] = await connection.execute('TRUNCATE TABLE `messages`');
    console.log('[OK] 已清空消息表');

    // 清空会话表
    const [conversationsResult] = await connection.execute('TRUNCATE TABLE `conversations`');
    console.log('[OK] 已清空会话表');

    // 重新启用外键检查
    await connection.execute('SET FOREIGN_KEY_CHECKS = 1');
    console.log('[OK] 已重新启用外键检查');

    console.log('\n[SUCCESS] 聊天数据已清空完成！');

  } catch (error) {
    console.error('[ERROR] 清空聊天数据失败:', error.message);
    throw error;
  } finally {
    connection.release();
    process.exit(0);
  }
}

clearChatData();
