// 举报路由
const express = require('express');
const router = express.Router();
const reportController = require('../controllers/reportController');
const auth = require('../middlewares/auth');

// 提交举报
router.post('/', auth.verify, reportController.create);

// 我的举报记录
router.get('/', auth.verify, reportController.getMyReports);

module.exports = router;
