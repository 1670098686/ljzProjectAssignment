// 数据库连接配置
const mysql = require('mysql2/promise');
const config = require('../config');

// 创建连接池
const pool = mysql.createPool({
  host: config.database.host,
  port: config.database.port,
  user: config.database.user,
  password: config.database.password,
  database: config.database.database,
  waitForConnections: config.database.waitForConnections,
  connectionLimit: config.database.connectionLimit,
  queueLimit: config.database.queueLimit,
  acquireTimeout: 60000,
  timeout: 60000,
  multipleStatements: false
});

// 测试连接
pool.getConnection()
  .then(connection => {
    console.log('MySQL 数据库连接成功');
    connection.release();
  })
  .catch(err => {
    console.error('MySQL 数据库连接失败:', err.message);
  });

module.exports = pool;
