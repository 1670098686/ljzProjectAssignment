require('dotenv').config();
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');

async function updatePassword() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
  });

  try {
    // 更新 user@example.com 的密码为 123456
    const hashedPassword = await bcrypt.hash('123456', 10);
    const [result] = await conn.execute(
      'UPDATE users SET password = ? WHERE email = ?',
      [hashedPassword, 'user@example.com']
    );

    console.log(`✓ 已更新 user@example.com 的密码为 123456`);
    console.log(`  影响行数: ${result.affectedRows}`);

    // 验证更新后的密码
    const [users] = await conn.execute('SELECT email, password FROM users WHERE email = ?', ['user@example.com']);
    if (users[0]) {
      const match = await bcrypt.compare('123456', users[0].password);
      console.log(`  验证结果: ${match ? '✓ 密码 123456 验证通过' : '✗ 验证失败'}`);
    }

    // 显示所有账号的登录信息
    console.log('\n当前可用账号：');
    const [allUsers] = await conn.execute('SELECT id, email, nickname, role, status FROM users');
    const accounts = [
      { email: 'admin@example.com', password: 'admin123' },
      { email: 'user@example.com', password: '123456' }
    ];
    accounts.forEach(acc => {
      const u = allUsers.find(x => x.email === acc.email);
      if (u) {
        console.log(`  ${u.nickname} (${u.role}): ${acc.email} / ${acc.password}`);
      }
    });
  } catch (err) {
    console.error('更新失败:', err.message);
    process.exit(1);
  } finally {
    await conn.end();
  }
}

updatePassword();
