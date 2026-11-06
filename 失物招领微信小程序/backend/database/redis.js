// Redis 客户端配置
const { createClient } = require('redis');
const config = require('../config');

const redisClient = createClient({
  socket: {
    host: config.redis.host,
    port: config.redis.port
  },
  password: config.redis.password || undefined
});

redisClient.on('error', err => {
  console.error('Redis 连接错误:', err.message);
});

redisClient.on('connect', () => {
  console.log('Redis 连接成功');
});

// 连接 Redis
redisClient.connect().catch(console.error);

module.exports = redisClient;
