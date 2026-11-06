// 认证中间件
const jwt = require('jsonwebtoken');
const config = require('../config');
const response = require('../utils/response');

/**
 * 验证 JWT token 中间件
 */
exports.verify = (req, res, next) => {
  const token = req.headers.authorization?.replace('Bearer ', '');
  
  if (!token) {
    return response.error(res, 401, '请先登录');
  }

  try {
    const decoded = jwt.verify(token, config.jwt.secret);
    req.user = decoded;
    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return response.error(res, 401, '登录已过期，请重新登录');
    }
    return response.error(res, 401, '无效的登录凭证');
  }
};

/**
 * 可选的认证中间件（不强制登录）
 */
exports.optional = (req, res, next) => {
  const token = req.headers.authorization?.replace('Bearer ', '');
  
  if (!token) {
    return next();
  }

  try {
    const decoded = jwt.verify(token, config.jwt.secret);
    req.user = decoded;
  } catch (error) {
    // 可选验证，token 无效时继续执行
  }
  
  next();
};

/**
 * 管理员权限验证中间件
 */
exports.requireAdmin = (req, res, next) => {
  if (!req.user || req.user.role !== 'ADMIN') {
    return response.error(res, 403, '需要管理员权限');
  }
  next();
};
