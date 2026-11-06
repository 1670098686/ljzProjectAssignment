// 数据库初始化脚本 - 用于重新创建所有表
require('dotenv').config();
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');
const config = require('./config');

async function initDatabase() {
  console.log('========================================');
  console.log('开始初始化数据库...');
  console.log('数据库配置:', {
    host: config.database.host,
    port: config.database.port,
    user: config.database.user,
    database: config.database.database
  });
  console.log('========================================');

  // 1. 先连接到 MySQL 服务器（不指定数据库）
  const serverConnection = await mysql.createConnection({
    host: config.database.host,
    port: config.database.port,
    user: config.database.user,
    password: config.database.password,
    multipleStatements: true
  });

  console.log('✓ 已连接到 MySQL 服务器');

  try {
    // 2. 删除并重建数据库
    await serverConnection.execute(`DROP DATABASE IF EXISTS ${config.database.database}`);
    console.log(`✓ 已删除旧数据库: ${config.database.database}`);

    await serverConnection.execute(
      `CREATE DATABASE ${config.database.database} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`
    );
    console.log(`✓ 已创建新数据库: ${config.database.database}`);

    await serverConnection.end();

    // 3. 读取 SQL 初始化脚本
    const sqlPath = path.join(__dirname, 'sql', 'init.sql');
    const sqlContent = fs.readFileSync(sqlPath, 'utf8');
    console.log('✓ 已读取 init.sql');

    // 4. 连接到新数据库并执行所有表创建语句
    const dbConnection = await mysql.createConnection({
      host: config.database.host,
      port: config.database.port,
      user: config.database.user,
      password: config.database.password,
      database: config.database.database,
      multipleStatements: true
    });

    // 移除脚本开头的 CREATE DATABASE 和 USE 语句（因为我们已经手动创建了）
    // 并按分号分割语句（简单做法：直接用 multipleStatements 执行整个脚本，但跳过前两行）
    let statementsToExecute = sqlContent
      .split('\n')
      .filter(line => {
        const trimmed = line.trim();
        return !trimmed.startsWith('CREATE DATABASE') && !trimmed.startsWith('USE ');
      })
      .join('\n');

    await dbConnection.query(statementsToExecute);
    console.log('✓ 已执行所有表创建语句');

    // 5. 验证表已创建
    const [tables] = await dbConnection.query('SHOW TABLES');
    console.log(`✓ 已创建 ${tables.length} 张表:`);
    tables.forEach(table => {
      const tableName = Object.values(table)[0];
      console.log(`   - ${tableName}`);
    });

    // 6. 插入测试数据：管理员账号
    // 密码: admin123 的 bcrypt hash（使用简单的值，后续可在应用中修改）
    // 这里使用 bcrypt 的 hash，需要先安装 bcrypt
    try {
      const bcrypt = require('bcrypt');
      const adminPassword = 'admin123';
      const hashedPassword = await bcrypt.hash(adminPassword, 10);

      await dbConnection.execute(
        `INSERT INTO users (openid, email, password, nickname, role, status) 
         VALUES (?, ?, ?, ?, ?, ?)`,
        ['admin_openid_demo', 'admin@example.com', hashedPassword, '管理员', 'ADMIN', 1]
      );
      console.log('✓ 已插入测试管理员账号 (email: admin@example.com, password: admin123)');
    } catch (err) {
      console.log('⚠ 无法创建测试管理员账号（bcrypt 未安装），请手动创建或忽略');
      console.log('  错误:', err.message);
    }

    // 7. 插入一个普通测试用户
    try {
      const bcrypt = require('bcrypt');
      const userPassword = 'user123';
      const hashedPassword = await bcrypt.hash(userPassword, 10);

      await dbConnection.execute(
        `INSERT INTO users (openid, email, password, nickname, role, status) 
         VALUES (?, ?, ?, ?, ?, ?)`,
        ['user_openid_demo', 'user@example.com', hashedPassword, '测试用户', 'USER', 1]
      );
      console.log('✓ 已插入测试普通用户账号 (email: user@example.com, password: 123456)');
    } catch (err) {
      console.log('⚠ 无法创建测试用户账号（bcrypt 未安装），请手动创建或忽略');
    }

    await dbConnection.end();

    console.log('========================================');
    console.log('✓ 数据库初始化完成！');
    console.log('========================================');
  } catch (error) {
    console.error('✗ 初始化失败:', error.message);
    process.exit(1);
  }
}

initDatabase();
