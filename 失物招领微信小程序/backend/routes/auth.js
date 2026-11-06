// 认证路由
const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const auth = require('../middlewares/auth');

// 微信登录
router.post('/wx-login', authController.wxLogin);

// 邮箱注册
router.post('/register', authController.register);

// 邮箱密码登录
router.post('/password-login', authController.passwordLogin);

// 发送邮箱验证码
router.post('/email-code/send', authController.sendEmailCode);

// 刷新 token
router.post('/refresh-token', authController.refreshToken);

// 获取当前用户信息
router.get('/me', auth.verify, authController.getMe);

// 更新当前用户信息
router.patch('/me', auth.verify, authController.updateMe);

// 获取我的发布列表
router.get('/me/items', auth.verify, authController.getMyItems);

// 检查登录状态（不需要认证）
router.get('/check-status', authController.checkLoginStatus);

module.exports = router;
