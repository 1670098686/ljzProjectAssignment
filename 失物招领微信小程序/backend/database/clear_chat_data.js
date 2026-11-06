// 清空聊天数据的脚本
const pool = require('./connection');

async function clearChatData() {
  console.log('开始清空聊天数据...');

  const connection = await pool.getConnection();

  try {
    // 禁用外键检查
    await connection.execute('SET FOREIGN_KEY_CHECKS = 0');
    console.log('✓ 已禁用外键检查');

    // 清空消息表
    const [messagesResult] = await connection.execute('TRUNCATE TABLE `messages`');
    console.log(`✓ 已清空消息表，影响 ${messagesResult.affectedRows} 行`);

    // 清空会话表
    const [conversationsResult] = await connection.execute('TRUNCATE TABLE `conversations`');
    console.log(`✓ 已清空会话表，影响 ${conversationsResult.affectedRows} 行`);

    // 重新启用外键检查
    await connection.execute('SET FOREIGN_KEY_CHECKS = 1');
    console.log('✓ 已重新启用外键检查');

    console.log('\n✅ 聊天数据已清空完成！');

  } catch (error) {
    console.error('❌ 清空聊天数据失败:', error.message);
    throw error;
  } finally {
    connection.release();
    process.exit(0);
  }
}

// 询问确认
const readline = require('readline');
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

console.log('⚠️  警告：此操作将删除所有聊天会话和消息数据！');
console.log('此操作不可恢复，请确认是否继续？');

rl.question('请输入 "yes" 确认：', (answer) => {
  rl.close();
  if (answer.trim().toLowerCase() === 'yes') {
    clearChatData();
  } else {
    console.log('操作已取消');
    process.exit(0);
  }
});
