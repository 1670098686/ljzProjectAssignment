// 日志中间件
const { v4: uuidv4 } = require('uuid');
const logger = require('../utils/logger');

module.exports = (req, res, next) => {
  // 生成请求 ID
  const requestId = uuidv4();
  req.requestId = requestId;
  res.requestId = requestId;

  // 记录请求开始时间
  const startTime = Date.now();

  // 记录请求日志
  logger.info('请求开始', {
    requestId,
    method: req.method,
    path: req.path,
    query: req.query,
    body: req.body,
    ip: req.ip
  });

  // 监听响应结束
  res.on('finish', () => {
    const duration = Date.now() - startTime;
    logger.info('请求结束', {
      requestId,
      method: req.method,
      path: req.path,
      statusCode: res.statusCode,
      duration: `${duration}ms`
    });
  });

  next();
};
