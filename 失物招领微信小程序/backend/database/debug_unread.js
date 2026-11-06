const pool = require('./connection');
const Conversation = require('../models/Conversation');

async function debugUnread() {
  console.log('🔍 开始调试未读计数功能...\n');

  try {
    const connection = await pool.getConnection();
    
    try {
      // 1. 查看所有消息
      const [allMessages] = await connection.execute(
        `SELECT id, conversation_id, sender_id, type, status, created_at 
         FROM messages 
         ORDER BY created_at DESC`
      );
      
      console.log('📨 所有消息:');
      console.table(allMessages.map(m => ({
        ID: m.id,
        '会话ID': m.conversation_id,
        '发送者ID': m.sender_id,
        '类型': m.type,
        '状态': m.status,
        '时间': m.created_at
      })));

      console.log('\n----------------------------------\n');

      // 2. 直接查询未读数量测试
      console.log('🔢 直接测试未读计数查询:');
      
      if (allMessages.length > 0) {
        // 获取第一个消息的会话ID和用户ID
        const testConversationId = allMessages[0].conversation_id;
        const testSenderId = allMessages[0].sender_id;
        
        // 计算非发送者的未读消息（模拟接收者视角）
        const [unreadTest] = await connection.execute(
          `SELECT COUNT(*) as count FROM messages 
           WHERE conversation_id = ? AND sender_id != ? AND (status != 'READ' OR status IS NULL)`,
          [testConversationId, testSenderId]
        );
        
        console.log(`   会话 ${testConversationId} 中，对于用户 ${testSenderId} 以外的人，未读消息: ${unreadTest[0].count}`);

        // 3. 用模型方法测试
        console.log('\n🎯 使用模型方法获取会话列表（包含未读计数）:');
        
        // 获取一个有效的用户ID（从消息中获取）
        const testUserId = allMessages[0].sender_id;
        
        const conversations = await Conversation.findByUserId(testUserId, 1, 20);
        
        console.log(`   找到 ${conversations.list.length} 个会话`);
        
        conversations.list.forEach((conv, index) => {
          console.log(`   ${index + 1}. 会话 ${conv.id}: 未读 ${conv.unreadCount} 条, 最后消息: ${conv.lastMessage}`);
        });
      }

      console.log('\n----------------------------------\n');

      // 4. 检查当前用户状态
      const [users] = await connection.execute('SELECT id, nickname FROM users LIMIT 10');
      console.log('👥 用户列表:');
      console.table(users);

    } finally {
      connection.release();
    }

  } catch (error) {
    console.error('❌ 调试失败:', error);
    console.error(error.stack);
  }

  process.exit(0);
}

debugUnread();
