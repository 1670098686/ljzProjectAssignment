// 文件上传路由
const express = require('express');
const router = express.Router();
const fileController = require('../controllers/fileController');
const auth = require('../middlewares/auth');

// 上传图片（需要认证）
router.post('/images', auth.verify, fileController.uploadImage);

module.exports = router;
