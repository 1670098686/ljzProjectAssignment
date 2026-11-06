// 管理员控制器
const response = require('../utils/response');
const Item = require('../models/Item');
const ItemImage = require('../models/ItemImage');
const User = require('../models/User');
const Report = require('../models/Report');
const Notification = require('../models/Notification');
const AuditLog = require('../models/AuditLog');
const pool = require('../database/connection');
const { formatImageUrl } = require('../utils/imageUrl');

function formatDateTimeForMySQL(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  const second = pad(date.getSeconds());
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

// 获取待审核列表
exports.getPendingItems = async (req, res) => {
  try {
    const { itemType, page = 1, pageSize = 20 } = req.query;
    
    const result = await Item.listPending(parseInt(page), parseInt(pageSize), itemType || undefined);

    const listWithImages = await enrichItemsWithImages(result.list, req);

    response.page(res, listWithImages, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取待审核列表失败：${error.message}`);
  }
};

// 获取已审核列表
exports.getAuditedItems = async (req, res) => {
  try {
    const { itemType, page = 1, pageSize = 20 } = req.query;

    const result = await Item.list(parseInt(page), parseInt(pageSize), {
      itemType: itemType || undefined,
      auditStatuses: ['APPROVED', 'REJECTED']
    });

    const listWithImages = await enrichItemsWithImages(result.list, req);

    response.page(res, listWithImages, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取已审核列表失败：${error.message}`);
  }
};

// 获取管理员待办提醒数量
exports.getReminderCounts = async (req, res) => {
  try {
    const [pendingItemRows] = await pool.execute("SELECT COUNT(*) as count FROM items WHERE audit_status = 'PENDING'");
    const [pendingReportRows] = await pool.execute("SELECT COUNT(*) as count FROM reports WHERE status = 'PENDING'");

    response.success(res, {
      pendingItemCount: pendingItemRows[0].count,
      pendingReportCount: pendingReportRows[0].count
    });
  } catch (error) {
    response.error(res, 500, `获取提醒数量失败：${error.message}`);
  }
};

// 管理员查看任意物品详情
exports.getItemDetail = async (req, res) => {
  try {
    const { id } = req.params;
    const item = await Item.findById(parseInt(id));

    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    const images = await ItemImage.findByItemId(item.id);
    const imageUrls = images.map(img => formatImageUrl(img.image_url, req));

    response.success(res, {
      id: item.id,
      userId: item.user_id,
      itemType: item.item_type,
      title: item.title,
      category: item.category,
      happenTime: item.happen_time,
      location: item.location,
      featureDesc: item.feature_desc,
      contactName: item.contact_name,
      contactPhone: item.contact_phone,
      contactWechat: item.contact_wechat,
      imageUrls,
      auditStatus: item.audit_status,
      bizStatus: item.biz_status,
      viewCount: item.view_count,
      publishedAt: item.published_at,
      visible: item.visible,
      isLocked: item.is_locked
    });
  } catch (error) {
    response.error(res, 500, `获取物品详情失败：${error.message}`);
  }
};

// 审核物品信息
exports.auditItem = async (req, res) => {
  try {
    const { id } = req.params;
    const { auditStatus, reason } = req.body;
    const adminId = req.user.id;
    
    if (!auditStatus || !['APPROVED', 'REJECTED'].includes(auditStatus)) {
      return response.error(res, 400, '审核状态参数错误');
    }

    const item = await Item.findById(parseInt(id));
    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    // 允许重复审核，不再检查是否已审核

    // 开启事务
    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();

      // 更新物品审核状态
      if (auditStatus === 'APPROVED') {
        await Item.updateAuditStatus(item.id, 'APPROVED', formatDateTimeForMySQL());
      } else {
        await Item.updateAuditStatus(item.id, 'REJECTED');
      }

      // 记录审核日志（使用正确的action枚举值）
      await AuditLog.create({
        item_id: item.id,
        auditor_id: adminId,
        action: auditStatus === 'APPROVED' ? 'APPROVE' : 'REJECT',
        reason
      });

      await connection.commit();
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }

    // 发送通知给用户（使用正确的type枚举值）
    try {
      if (auditStatus === 'APPROVED') {
        await Notification.create(
          item.user_id,
          'AUDIT',
          '审核通过',
          `您发布的「${item.title}」已通过审核，现在可以被其他用户看到。`,
          item.id
        );
      } else {
        await Notification.create(
          item.user_id,
          'AUDIT',
          '审核驳回',
          `您发布的「${item.title}」未通过审核。${reason ? '原因：' + reason : ''}`,
          item.id
        );
      }
    } catch (notifyError) {
      console.error('发送通知失败:', notifyError);
    }

    response.success(res, {
      id: item.id,
      auditStatus,
      auditedAt: new Date().toISOString()
    }, '审核完成');
  } catch (error) {
    response.error(res, 500, `审核失败：${error.message}`);
  }
};

// 信息管理（下架/恢复）
exports.toggleItemVisibility = async (req, res) => {
  try {
    const { id } = req.params;
    const { visible } = req.body;
    
    if (typeof visible !== 'boolean') {
      return response.error(res, 400, 'visible 参数错误');
    }

    const item = await Item.findById(parseInt(id));
    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    await Item.update(item.id, { visible: visible ? 1 : 0 });

    response.success(res, null, visible ? '信息已恢复上架' : '信息已下架');
  } catch (error) {
    response.error(res, 500, `操作失败：${error.message}`);
  }
};

// 举报列表
exports.getReports = async (req, res) => {
  try {
    const { status, page = 1, pageSize = 20 } = req.query;
    
    const result = await Report.listAll(parseInt(page), parseInt(pageSize), status || undefined);

    response.page(res, result.list, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取举报列表失败：${error.message}`);
  }
};

// 处理举报
exports.handleReport = async (req, res) => {
  try {
    const { id } = req.params;
    const { action, note } = req.body;
    const adminId = req.user.id;

    if (!action || !['REMOVE', 'REJECT', 'IGNORE'].includes(action)) {
      return response.error(res, 400, '处理动作参数错误');
    }

    const report = await Report.findById(parseInt(id));
    if (!report) {
      return response.error(res, 404, '举报记录不存在');
    }

    if (report.status !== 'PENDING') {
      return response.error(res, 409, '该举报已处理，无法重复处理');
    }

    const newStatus = action === 'REMOVE' ? 'PROCESSED' : 'REJECTED';
    await Report.updateStatus(report.id, newStatus, action, note || null, adminId);

    const item = await Item.findById(report.item_id);
    const itemTitle = (item && item.title) || '该物品';

    if (action === 'REMOVE') {
      // 下架物品并将审核状态改为已驳回
      await Item.update(report.item_id, {
        visible: 0,
        audit_status: 'REJECTED'
      });

      // 发送系统通知给物品发布者（除IGNORE外，其他处理结果都要通知发布者）
      if (item && item.user_id) {
        const noteText = note ? `处理说明：${note}` : '';
        await Notification.create(
          item.user_id,
          'SYSTEM',
          '物品已下架',
          `您发布的「${itemTitle}」因被举报违规已被管理员下架处理。${noteText}如有疑问请联系管理员。`,
          item.id
        );
      }

      // 发送系统通知给举报人（所有处理结果都要通知举报人）
      if (report.reporter_id) {
        const noteText = note ? `处理说明：${note}` : '物品已下架处理';
        await Notification.create(
          report.reporter_id,
          'SYSTEM',
          '举报处理结果',
          `感谢您的举报，您举报的「${itemTitle}」已由管理员处理。${noteText}。`,
          report.item_id
        );
      }
    } else if (action === 'REJECT') {
      // 拒绝举报（驳回举报），通知物品发布者和举报人
      if (item && item.user_id) {
        const noteText = note ? `处理说明：${note}` : '';
        await Notification.create(
          item.user_id,
          'SYSTEM',
          '举报核查结果',
          `您发布的「${itemTitle}」已通过举报核查，未发现违规内容。${noteText}`,
          item.id
        );
      }

      if (report.reporter_id) {
        const noteText = note ? `处理说明：${note}` : '经核查未发现违规内容';
        await Notification.create(
          report.reporter_id,
          'SYSTEM',
          '举报处理结果',
          `感谢您的举报，您举报的「${itemTitle}」已由管理员核查。${noteText}。`,
          report.item_id
        );
      }
    } else if (action === 'IGNORE') {
      // 忽略举报：仅通知举报人（不通知发布者，不做任何物品状态变更）
      if (report.reporter_id) {
        const noteText = note ? `处理说明：${note}` : '经核查未发现违规内容';
        await Notification.create(
          report.reporter_id,
          'SYSTEM',
          '举报处理结果',
          `感谢您的举报，您举报的「${itemTitle}」已由管理员核查。${noteText}。`,
          report.item_id
        );
      }
    }

    response.success(res, null, '举报处理完成');
  } catch (error) {
    response.error(res, 500, `处理举报失败：${error.message}`);
  }
};

// 用户列表
exports.getUserList = async (req, res) => {
  try {
    const { role, status, page = 1, pageSize = 20 } = req.query;
    
    const filters = {};
    if (role) filters.role = role;
    if (status !== undefined) filters.status = parseInt(status);

    const result = await User.list(parseInt(page), parseInt(pageSize), filters);

    response.page(res, result.list, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取用户列表失败：${error.message}`);
  }
};

// 禁用/启用用户
exports.toggleUserStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;
    
    if (![0, 1].includes(status)) {
      return response.error(res, 400, '状态参数错误');
    }

    await User.updateStatus(parseInt(id), status);

    response.success(res, null, status === 1 ? '用户已启用' : '用户已禁用');
  } catch (error) {
    response.error(res, 500, `操作失败：${error.message}`);
  }
};

// 统计数据
exports.getStatistics = async (req, res) => {
  try {
    // 获取各项统计数据
    const [totalRows] = await pool.execute('SELECT COUNT(*) as count FROM items');
    const [lostRows] = await pool.execute("SELECT COUNT(*) as count FROM items WHERE item_type = 'LOST'");
    const [foundRows] = await pool.execute("SELECT COUNT(*) as count FROM items WHERE item_type = 'FOUND'");
    const [approvedRows] = await pool.execute("SELECT COUNT(*) as count FROM items WHERE audit_status = 'APPROVED'");
    const [resolvedRows] = await pool.execute("SELECT COUNT(*) as count FROM items WHERE biz_status IN ('RESOLVED', 'RETURNED')");
    const [totalReportRows] = await pool.execute('SELECT COUNT(*) as count FROM reports');
    const [pendingReportRows] = await pool.execute("SELECT COUNT(*) as count FROM reports WHERE status = 'PENDING'");

    response.success(res, {
      totalItems: totalRows[0].count,
      lostCount: lostRows[0].count,
      foundCount: foundRows[0].count,
      approvedRate: totalRows[0].count > 0 ? (approvedRows[0].count / totalRows[0].count).toFixed(2) : 0,
      resolvedOrReturnedCount: resolvedRows[0].count,
      reportProcessedRate: totalReportRows[0].count > 0 ? ((totalReportRows[0].count - pendingReportRows[0].count) / totalReportRows[0].count).toFixed(2) : 0
    });
  } catch (error) {
    response.error(res, 500, `获取统计数据失败：${error.message}`);
  }
};

// 发送系统通知
exports.sendSystemNotification = async (req, res) => {
  try {
    const { title, content, targetUserIds, sendToAll = false } = req.body;
    const adminId = req.user.id;

    if (!title || !content) {
      return response.error(res, 400, '标题和内容不能为空');
    }

    if (title.length > 128) {
      return response.error(res, 400, '标题不能超过128个字符');
    }

    let userIds = [];
    
    if (sendToAll) {
      // 发送给所有用户（包括管理员自己）
      userIds = await Notification.getAllUserIds();
    } else if (targetUserIds && targetUserIds.length > 0) {
      // 发送给指定用户
      userIds = targetUserIds;
    } else {
      return response.error(res, 400, '请选择接收通知的用户');
    }

    if (userIds.length === 0) {
      return response.error(res, 400, '没有可接收通知的用户');
    }

    // 批量创建通知
    const sentCount = await Notification.createBatch(userIds, 'SYSTEM', title, content);

    response.success(res, {
      sentCount,
      totalUsers: userIds.length
    }, `通知发送成功，已发送给 ${sentCount} 位用户`);
  } catch (error) {
    response.error(res, 500, `发送通知失败：${error.message}`);
  }
};

async function enrichItemsWithImages(items, req) {
  return Promise.all(items.map(async (item) => {
    const images = await ItemImage.findByItemId(item.id);
    return {
      ...item,
      itemType: item.item_type,
      auditStatus: item.audit_status,
      bizStatus: item.biz_status,
      happenTime: item.happen_time,
      publishedAt: item.published_at,
      imageUrls: images.map(image => formatImageUrl(image.image_url, req))
    };
  }));
}
