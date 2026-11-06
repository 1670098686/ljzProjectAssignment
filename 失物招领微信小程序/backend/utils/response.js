// 统一响应格式工具
const { v4: uuidv4 } = require('uuid');

const response = {
  /**
   * 成功响应
   */
  success(res, data = null, message = 'ok') {
    return res.status(200).json({
      code: 0,
      message,
      data,
      requestId: res.requestId || uuidv4(),
      timestamp: new Date().toISOString()
    });
  },

  /**
   * 错误响应
   */
  error(res, statusCode = 500, message = '服务器内部错误', data = null) {
    return res.status(statusCode).json({
      code: statusCode,
      message,
      data,
      requestId: res.requestId || uuidv4(),
      timestamp: new Date().toISOString()
    });
  },

  /**
   * 分页响应
   */
  page(res, list = [], total = 0, page = 1, pageSize = 10, message = 'ok') {
    const totalPages = Math.ceil(total / pageSize);
    return res.status(200).json({
      code: 0,
      message,
      data: {
        list,
        page: parseInt(page),
        pageSize: parseInt(pageSize),
        total: parseInt(total),
        totalPage: totalPages,
        totalPages: totalPages
      },
      requestId: res.requestId || uuidv4(),
      timestamp: new Date().toISOString()
    });
  }
};

module.exports = response;
