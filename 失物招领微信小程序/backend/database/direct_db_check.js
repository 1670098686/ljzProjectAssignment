const pool = require('./connection');

async function directCheck() {
  console.log('🔍 开始直接数据库检查...');

  try {
    const connection = await pool.getConnection();
    
    try {
      // 1. 查询所有会话
      const [conversations] = await connection.execute('SELECT * FROM conversations');
      console.log('\n📞 所有会话:');
      console.table(conversations);

      // 2. 查询所有消息
      const [messages] = await connection.execute('SELECT * FROM messages ORDER BY id');
      console.log('\n📨 所有消息:');
      console.table(messages);

      // 3. 检查会话2的消息
      if (conversations.some(c => c.id === 2)) {
        console.log('\n🔎 检查会话 2 的消息:');
        const [conv2Messages] = await connection.execute(
          'SELECT * FROM messages WHERE conversation_id = ?', [2]
        );
        console.table(conv2Messages);
      }

      // 4. 检查会话1的消息
      if (conversations.some(c => c.id === 1)) {
        console.log('\n🔎 检查会话 1 的消息:');
        const [conv1Messages] = await connection.execute(
          'SELECT * FROM messages WHERE conversation_id = ?', [1]
        );
        console.table(conv1Messages);
      }

      // 5. 为用户4计算未读
      console.log('\n🧮 为用户4计算未读计数:');
      
      for (const conv of conversations) {
        const [unreadRes] = await connection.execute(
          `SELECT COUNT(*) AS cnt FROM messages
           WHERE conversation_id = ? 
           AND sender_id != ? 
           AND (status != 'READ' OR status IS NULL)`,
          [conv.id, 4]
        );
        
        const [lastMsg] = await connection.execute(
          `SELECT * FROM messages 
           WHERE conversation_id = ?
           ORDER BY created_at DESC 
           LIMIT 1`,
          [conv.id]
        );
        
        console.log(`  会话 ${conv.id}: 未读 ${unreadRes[0].cnt} 条`);
        if (lastMsg.length > 0) {
          console.log(`   最后消息: ID=${lastMsg[0].id}, sender=${lastMsg[0].sender_id}, content=${lastMsg[0].content}`);
        }
      }

    } finally {
      connection.release();
    }

  } catch (error) {
    console.error('❌ 查询失败:', error);
    console.error(error.stack);
  }

  process.exit(0);
}

directCheck();
