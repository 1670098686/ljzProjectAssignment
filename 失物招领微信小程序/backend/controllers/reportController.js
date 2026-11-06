// 举报控制器
const response = require('../utils/response');
const Report = require('../models/Report');
const Item = require('../models/Item');

// 提交举报
exports.create = async (req, res) => {
  try {
    const userId = req.user.id;
    const { itemId, reasonType, reasonDetail, evidenceUrls } = req.body;
    
    if (!itemId || !reasonType) {
      return response.error(res, 400, '缺少必要参数');
    }

    if (!['SCAM', 'AD', 'SENSITIVE', 'OTHER'].includes(reasonType)) {
      return response.error(res, 400, '举报原因类型参数错误');
    }

    // 检查物品是否存在
    const item = await Item.findById(itemId);
    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    const reportId = await Report.create({
      item_id: itemId,
      reporter_id: userId,
      reason_type: reasonType,
      reason: reasonDetail,
      evidence_urls: evidenceUrls
    });

    response.success(res, {
      reportId,
      status: 'PENDING'
    }, '举报提交成功');
  } catch (error) {
    response.error(res, 500, `举报失败：${error.message}`);
  }
};

// 我的举报记录
exports.getMyReports = async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, pageSize = 20 } = req.query;
    
    const result = await Report.findByUserId(userId, parseInt(page), parseInt(pageSize));

    response.page(res, result.list, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取举报记录失败：${error.message}`);
  }
};
