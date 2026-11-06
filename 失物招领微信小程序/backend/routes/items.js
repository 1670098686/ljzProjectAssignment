// 物品信息路由
const express = require('express');
const router = express.Router();
const itemController = require('../controllers/itemController');
const auth = require('../middlewares/auth');

// 发布物品信息
router.post('/', auth.verify, itemController.create);

// 获取物品列表
router.get('/', itemController.getList);

// 获取物品详情
router.get('/:id', auth.optional, itemController.getDetail);

// 更新物品信息
router.patch('/:id', auth.verify, itemController.update);

// 更新业务状态（找回/归还/取消）
router.patch('/:id/biz-status', auth.verify, itemController.updateBizStatus);

// 增加浏览次数
router.patch('/:id/view', itemController.addView);

module.exports = router;
