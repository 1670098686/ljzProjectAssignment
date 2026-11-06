const pool = require('./connection');

async function fixMessages() {
  console.log('🔧 开始修复消息与会话关联...');

  try {
    const connection = await pool.getConnection();
    
    try {
      console.log('步骤1: 查看当前所有会话和消息...');
      
      const [conversations] = await connection.execute('SELECT * FROM conversations');
      const [messages] = await connection.execute('SELECT * FROM messages ORDER BY id');
      
      console.log('\n📞 会话列表:');
      console.table(conversations);
      console.log('\n📨 消息列表:');
      console.table(messages);

      console.log('\n步骤2: 修复消息关联...');
      
      // 根据 item_id 和 参与用户来匹配会话
      // 会话 1: user1=1, user2=4, item=15 -> 匹配消息 1,2,3,4
      // 会话 2: user1=1, user2=4, item=13 -> 匹配消息5,6,7,8
      
      const fixSQL = [
        "UPDATE messages SET conversation_id = 1 WHERE id IN (1,2,3,4)",
        "UPDATE messages SET conversation_id = 2 WHERE id IN (5,6,7,8)"
      ];
      
      for (const sql of fixSQL) {
        console.log('执行:', sql);
        await connection.execute(sql);
      }
      
      console.log('\n✅ 修复完成！');
      
      console.log('\n步骤3: 验证修复结果...');
      
      const [fixedMessages] = await connection.execute(
        'SELECT * FROM messages ORDER BY id'
      );
      
      console.log('\n✅ 修复后的消息:');
      console.table(fixedMessages);
      
      console.log('\n步骤4: 检查会话1的消息...');
      const [conv1Msgs] = await connection.execute(
        'SELECT * FROM messages WHERE conversation_id = 1'
      );
      console.table(conv1Msgs);
      
      console.log('\n步骤5: 检查会话2的消息...');
      const [conv2Msgs] = await connection.execute(
        'SELECT * FROM messages WHERE conversation_id = 2'
      );
      console.table(conv2Msgs);
      
      console.log('\n🎉 修复成功！');
      
    } finally {
      connection.release();
    }

  } catch (error) {
    console.error('❌ 修复失败:', error);
    console.error(error.stack);
  }

  process.exit(0);
}

fixMessages();
