// 管理员路由
const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const auth = require('../middlewares/auth');

// 所有管理员接口都需要验证
router.use(auth.verify);
router.use(auth.requireAdmin);

// 审核管理
router.get('/items/pending', adminController.getPendingItems);
router.get('/items/audited', adminController.getAuditedItems);
router.get('/items/:id', adminController.getItemDetail);
router.patch('/items/:id/audit', adminController.auditItem);
router.patch('/items/:id/visibility', adminController.toggleItemVisibility);

// 举报管理
router.get('/reports', adminController.getReports);
router.patch('/reports/:id/handle', adminController.handleReport);

// 用户管理
router.get('/users', adminController.getUserList);
router.patch('/users/:id/status', adminController.toggleUserStatus);

// 统计数据
router.get('/statistics', adminController.getStatistics);

// 管理员待办提醒
router.get('/reminder-counts', adminController.getReminderCounts);

// 发送系统通知
router.post('/notifications/send', adminController.sendSystemNotification);

module.exports = router;
