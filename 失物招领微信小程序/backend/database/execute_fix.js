const pool = require('./connection');

async function fixMessagesStatus() {
  console.log('🔧 开始修复消息状态...');

  try {
    // 获取连接
    const connection = await pool.getConnection();
    
    try {
      // 第一步：查看当前NULL status的消息数量
      const [nullStatusResult] = await connection.execute(
        "SELECT COUNT(*) as null_count FROM messages WHERE status IS NULL"
      );
      console.log(`📊 发现 ${nullStatusResult[0].null_count} 条消息的状态为NULL`);

      if (nullStatusResult[0].null_count > 0) {
        // 第二步：更新NULL的status为'SENT'
        const [updateResult] = await connection.execute(
          "UPDATE messages SET status = 'SENT' WHERE status IS NULL"
        );
        console.log(`✅ 成功更新了 ${updateResult.affectedRows} 条消息`);

        // 第三步：验证更新结果
        const [afterResult] = await connection.execute(
          "SELECT COUNT(*) as null_count FROM messages WHERE status IS NULL"
        );
        console.log(`📊 更新后，NULL状态的消息数量: ${afterResult[0].null_count}`);

        // 第四步：显示最新的20条消息
        const [messages] = await connection.execute(
          `SELECT id, conversation_id, sender_id, type, status, created_at 
           FROM messages 
           ORDER BY created_at DESC 
           LIMIT 20`
        );
        console.log('\n📋 最新消息预览:');
        console.table(messages.map(m => ({
          ID: m.id,
          '会话ID': m.conversation_id,
          '发送者ID': m.sender_id,
          '类型': m.type,
          '状态': m.status,
          '时间': m.created_at
        })));

      } else {
        console.log('✅ 没有需要修复的消息');
      }

      console.log('\n🎉 消息状态修复完成！');

    } finally {
      connection.release();
    }

  } catch (error) {
    console.error('❌ 修复失败:', error.message);
    process.exit(1);
  }

  process.exit(0);
}

fixMessagesStatus();
