require('dotenv').config();
const mysql = require('mysql2/promise');

async function fixEnums() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    multipleStatements: true
  });

  try {
    console.log('修复 audit_logs.action 枚举值...');
    await conn.execute("ALTER TABLE audit_logs MODIFY COLUMN action ENUM('APPROVE', 'REJECT') NOT NULL COMMENT '审核动作'");
    console.log('✓ audit_logs.action 已修复为 APPROVE/REJECT');

    console.log('修复 notifications.type 枚举值...');
    await conn.execute("ALTER TABLE notifications MODIFY COLUMN type ENUM('AUDIT', 'SYSTEM', 'MESSAGE') NOT NULL COMMENT '通知类型'");
    console.log('✓ notifications.type 已修复为 AUDIT/SYSTEM/MESSAGE');

    console.log('修复 reports.status 枚举值...');
    await conn.execute("ALTER TABLE reports MODIFY COLUMN status ENUM('PENDING', 'PROCESSED', 'REJECTED') DEFAULT 'PENDING' COMMENT '处理状态'");
    console.log('✓ reports.status 已修复为 PENDING/PROCESSED/REJECTED');

    console.log('\n验证表结构...');
    const [tables] = await conn.query("SHOW TABLES");
    console.log(`数据库中有 ${tables.length} 张表：`);
    tables.forEach(t => {
      console.log(`  - ${Object.values(t)[0]}`);
    });

    console.log('\n✓ 所有枚举值修复完成！');
  } catch (err) {
    console.error('修复失败:', err.message);
    process.exit(1);
  } finally {
    await conn.end();
  }
}

fixEnums();
