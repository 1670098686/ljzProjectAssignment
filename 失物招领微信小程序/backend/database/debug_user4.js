const pool = require('./connection');
const Conversation = require('../models/Conversation');
const Message = require('../models/Message');

async function debugUser4() {
  console.log('🔍 开始调试用户ID=4的未读计数...\n');

  try {
    const connection = await pool.getConnection();
    
    try {
      const testUserId = 4;
      console.log(`👤 测试用户ID: ${testUserId}`);
      console.log('\n----------------------------------\n');

      // 1. 查看这个用户的所有会话中的消息
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

      // 2. 测试未读计数逻辑
      console.log('🔢 为用户 4 计算未读计数:');
      
      // 获取所有会话
      const [conversations] = await connection.execute(
        `SELECT * FROM conversations WHERE user1_id = ? OR user2_id = ?`,
        [testUserId, testUserId]
      );
      
      console.log(`   用户4参与的会话: ${conversations.length} 个`);
      
      for (const conv of conversations) {
        console.log(`   - 会话 ${conv.id}: user1=${conv.user1_id}, user2=${conv.user2_id}`);
        
        // 计算未读数量
        const [unreadResult] = await connection.execute(
          `SELECT COUNT(*) as count FROM messages 
           WHERE conversation_id = ? AND sender_id != ? AND (status != 'READ' OR status IS NULL)`,
          [conv.id, testUserId]
        );
        
        console.log(`     未读消息: ${unreadResult[0].count}`);
      }

      console.log('\n----------------------------------\n');
      
      // 3. 用模型方法测试
      console.log('🎯 使用Conversation模型获取会话:');
      
      const convList = await Conversation.findByUserId(testUserId, 1, 20);
      
      console.log(`   找到 ${convList.list.length} 个会话`);
      
      convList.list.forEach((conv, index) => {
        console.log(`   ${index + 1}. 会话 ${conv.id}: 未读 ${conv.unreadCount} 条, 最后消息: ${conv.lastMessage}`);
      });

    } finally {
      connection.release();
    }

  } catch (error) {
    console.error('❌ 调试失败:', error);
    console.error(error.stack);
  }

  process.exit(0);
}

debugUser4();
