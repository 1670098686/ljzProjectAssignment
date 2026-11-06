// 数据库迁移脚本：修复会话表唯一键约束
// 执行方式：node migrate_fix_conversation_key.js

const pool = require('./connection');

async function migrate() {
  console.log('开始执行数据库迁移...');
  
  const connection = await pool.getConnection();
  
  try {
    // 第一步：删除旧的唯一键约束（如果存在）
    console.log('步骤 1/3: 删除旧的唯一键约束...');
    try {
      await connection.execute('ALTER TABLE `conversations` DROP INDEX `uk_user1_user2`');
      console.log('✓ 成功删除旧的唯一键约束');
    } catch (err) {
      if (err.code === 'ER_DROP_INDEX') {
        console.log('→ 旧的唯一键约束不存在，跳过此步骤');
      } else {
        throw err;
      }
    }
    
    // 第二步：添加新的唯一键约束
    console.log('步骤 2/3: 添加新的唯一键约束...');
    try {
      await connection.execute('ALTER TABLE `conversations` ADD UNIQUE KEY `uk_user1_user2_item` (`user1_id`, `user2_id`, `item_id`)');
      console.log('✓ 成功添加新的唯一键约束');
    } catch (err) {
      if (err.code === 'ER_DUP_KEYNAME') {
        console.log('→ 新的唯一键约束已存在，跳过此步骤');
      } else {
        throw err;
      }
    }
    
    // 第三步：添加物品索引
    console.log('步骤 3/3: 添加物品索引...');
    try {
      await connection.execute('ALTER TABLE `conversations` ADD INDEX `idx_item_id` (`item_id`)');
      console.log('✓ 成功添加物品索引');
    } catch (err) {
      if (err.code === 'ER_DUP_KEYNAME') {
        console.log('→ 物品索引已存在，跳过此步骤');
      } else {
        throw err;
      }
    }
    
    console.log('\n✓ 数据库迁移成功完成！');
    
  } catch (err) {
    console.error('\n✗ 数据库迁移失败:', err.message);
    throw err;
  } finally {
    connection.release();
    process.exit(0);
  }
}

migrate();
