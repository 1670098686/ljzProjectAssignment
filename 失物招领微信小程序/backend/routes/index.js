// 路由入口文件
const express = require('express');
const router = express.Router();

const authRoutes = require('./auth');
const itemRoutes = require('./items');
const fileRoutes = require('./files');
const messageRoutes = require('./messages');
const reportRoutes = require('./reports');
const adminRoutes = require('./admin');

// 注册路由
router.use('/auth', authRoutes);
router.use('/items', itemRoutes);
router.use('/files', fileRoutes);
router.use('/messages', messageRoutes);
router.use('/reports', reportRoutes);
router.use('/admin', adminRoutes);

module.exports = router;
