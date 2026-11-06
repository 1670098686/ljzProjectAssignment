const pool = require('./connection');

async function forceFix() {
  console.log('🔧 强制修复开始！');

  try {
    const connection = await pool.getConnection();
    
    try {
      console.log('执行强制修复SQL...');
      
      await connection.execute("UPDATE messages SET conversation_id = 1 WHERE id IN (1, 2, 3, 4)");
      await connection.execute("UPDATE messages SET conversation_id = 2 WHERE id IN (5, 6, 7, 8)");
      
      console.log('✅ SQL执行完成！');
      
      console.log('\n验证修复结果...');
      const [result] = await connection.execute('SELECT * FROM messages ORDER BY id');
      console.log('\n所有消息:');
      result.forEach(m => {
        console.log(`ID ${m.id}: conv=${m.conversation_id}, sender=${m.sender_id}`);
      });
      
      console.log('\n✅ 强制修复完成！');
      
    } finally {
      connection.release();
    }

  } catch (error) {
    console.error('❌ 修复失败:', error);
    console.error(error.stack);
  }

  process.exit(0);
}

forceFix();
