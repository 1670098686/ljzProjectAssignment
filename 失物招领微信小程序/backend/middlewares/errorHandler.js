// 全局错误处理中间件
const logger = require('../utils/logger');
const response = require('../utils/response');

module.exports = (err, req, res, next) => {
  // 记录错误日志
  logger.error('服务器错误', {
    requestId: req.requestId,
    message: err.message,
    stack: err.stack
  });

  // 处理不同类型的错误
  if (err.name === 'ValidationError') {
    // 参数校验错误
    return response.error(res, err.message, 400);
  }

  if (err.name === 'UnauthorizedError') {
    // JWT 认证错误
    return response.error(res, '未授权访问', 401);
  }

  if (err.code === 'ER_DUP_ENTRY') {
    // 数据库唯一键冲突
    return response.error(res, '数据已存在', 400);
  }

  // 默认服务器错误
  const statusCode = err.statusCode || err.status || 500;
  const message = statusCode === 500 ? '服务器内部错误' : err.message;

  response.error(res, message, statusCode);
};
