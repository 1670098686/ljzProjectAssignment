// 用户路由
const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const auth = require('../middlewares/auth');

// 获取当前用户信息
router.get('/me', auth.verify, userController.getMe);

// 更新当前用户信息
router.put('/me', auth.verify, userController.updateMe);

// 获取我的发布列表
router.get('/me/items', auth.verify, userController.getMyItems);

// 上传头像
router.post('/me/avatar', auth.verify, userController.uploadAvatar);

module.exports = router;
