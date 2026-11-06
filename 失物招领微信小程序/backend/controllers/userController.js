// 用户控制器
const response = require('../utils/response');

exports.getMe = async (req, res) => {
  try {
    const userId = req.user.id;
    
    // TODO: 获取当前用户详细信息
    
    response.success(res, {});
  } catch (error) {
    response.error(res, error.message, 500);
  }
};

exports.updateMe = async (req, res) => {
  try {
    const userId = req.user.id;
    const data = req.body;
    
    // TODO: 更新当前用户信息
    
    response.success(res, { message: '更新成功' });
  } catch (error) {
    response.error(res, error.message, 500);
  }
};

exports.getMyItems = async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, pageSize = 10 } = req.query;
    
    // TODO: 获取我的发布列表
    
    response.page(res, [], 0, page, pageSize);
  } catch (error) {
    response.error(res, error.message, 500);
  }
};

exports.uploadAvatar = async (req, res) => {
  try {
    const userId = req.user.id;
    
    // TODO: 实现上传头像逻辑
    
    response.success(res, { avatarUrl: 'https://example.com/avatar.jpg' });
  } catch (error) {
    response.error(res, error.message, 500);
  }
};
