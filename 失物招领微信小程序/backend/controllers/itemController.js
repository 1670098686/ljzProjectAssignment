// 物品信息控制器
const response = require('../utils/response');
const Item = require('../models/Item');
const ItemImage = require('../models/ItemImage');
const User = require('../models/User');
const config = require('../config');
const { formatImageUrl, formatImageUrls } = require('../utils/imageUrl');

// 创建物品信息（发布）
exports.create = async (req, res) => {
  try {
    const userId = req.user.id;
    const {
      itemType,
      title,
      category,
      happenTime,
      location,
      featureDesc,
      imageUrls,
      contactName,
      contactPhone,
      contactWechat
    } = req.body;
    
    // 参数校验
    if (!itemType || !title || !category || !happenTime || !location || !featureDesc) {
      return response.error(res, 400, '缺少必要参数');
    }
    
    if (!['LOST', 'FOUND'].includes(itemType)) {
      return response.error(res, 400, '物品类型参数错误');
    }

    // 数据库 contact_name 为必填，优先使用请求参数，其次用当前用户昵称兜底
    const currentUser = await User.findById(userId);
    const finalContactName = (contactName && String(contactName).trim()) ||
      (currentUser?.nickname && String(currentUser.nickname).trim()) ||
      `用户${userId}`;

    // 转换日期格式
    let formattedHappenTime = happenTime;
    try {
      const date = new Date(happenTime);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      const second = String(date.getSeconds()).padStart(2, '0');
      formattedHappenTime = `${year}-${month}-${day} ${hour}:${minute}:${second}`;
    } catch (e) {
      console.error('日期格式转换失败:', e);
    }

    // 创建物品信息
    const itemId = await Item.create({
      user_id: userId,
      item_type: itemType,
      title,
      category,
      happen_time: formattedHappenTime,
      location,
      feature_desc: featureDesc,
      contact_name: finalContactName,
      contact_phone: contactPhone || currentUser?.phone || null,
      contact_wechat: contactWechat || null
    });

    // 保存图片
    if (imageUrls && imageUrls.length > 0) {
      await ItemImage.createBatch(itemId, imageUrls);
    }

    response.success(res, {
      id: itemId,
      auditStatus: 'PENDING',
      bizStatus: 'ONGOING',
      createdAt: new Date().toISOString()
    }, '发布成功，待审核');
  } catch (error) {
    response.error(res, 500, `发布失败：${error.message}`);
  }
};

// 获取物品列表
exports.getList = async (req, res) => {
  try {
    const { itemType, keyword, category, location, startTime, endTime, 
            sortBy, page = 1, pageSize = 10, status } = req.query;
    
    const filters = {
      itemType: itemType || undefined,
      keyword: keyword || undefined,
      category: category || undefined,
      location: location || undefined,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      sortBy: sortBy || 'latest'
    };

    // 处理状态筛选 - 不传 status 时，默认只显示进行中信息
    if (status === 'ONGOING') {
      filters.bizStatus = 'ONGOING';
    } else if (status === 'COMPLETED') {
      // 已完成状态包括已找回、已归还
      filters.bizStatuses = ['RESOLVED', 'RETURNED'];
    } else {
      // 默认：未传状态筛选（进行中）
      filters.bizStatus = 'ONGOING';
    }
    
    const result = await Item.list(parseInt(page), parseInt(pageSize), filters);

    // 为每个物品获取封面图
    const listWithImages = await Promise.all(result.list.map(async (item) => {
      const images = await ItemImage.findByItemId(item.id);
      return {
        id: item.id,
        itemType: item.item_type,
        title: item.title,
        category: item.category,
        happenTime: item.happen_time,
        location: item.location,
        coverImage: images.length > 0 ? formatImageUrl(images[0].image_url, req) : null,
        bizStatus: item.biz_status,
        publishedAt: item.published_at
      };
    }));

    response.page(res, listWithImages, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取列表失败：${error.message}`);
  }
};

// 获取物品详情
exports.getDetail = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user?.id;
    const userRole = req.user?.role;
    
    // 获取物品信息
    const item = await Item.findById(parseInt(id));
    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    // 只有审核通过的才能公开查看（非发布者和管理员）
    if (item.audit_status !== 'APPROVED') {
      if (!userId || (userId !== item.user_id && userRole !== 'ADMIN')) {
        return response.error(res, 403, '该信息未审核，暂时无法查看');
      }
    }

    // 增加浏览次数
    await Item.incrementViewCount(item.id);

    // 获取图片
    const images = await ItemImage.findByItemId(item.id);
    const imageUrls = formatImageUrls(images.map(img => img.image_url), req);

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
      publishedAt: item.published_at
    });
  } catch (error) {
    response.error(res, 500, `获取详情失败：${error.message}`);
  }
};

// 更新物品信息
exports.update = async (req, res) => {
  try {
    const userId = req.user.id;
    const { id } = req.params;
    
    // 获取物品信息
    const item = await Item.findById(parseInt(id));
    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    // 权限校验：仅本人可编辑
    if (item.user_id !== userId) {
      return response.error(res, 403, '无权编辑该信息');
    }

    // 状态校验：仅未完成的信息可编辑（已驳回的也允许编辑）
    const completedStatuses = ['RESOLVED', 'RETURNED', 'CANCELLED'];
    if (completedStatuses.includes(item.biz_status)) {
      return response.error(res, 409, '已完成的信息不可编辑');
    }

    // 锁定校验
    if (item.is_locked === 1) {
      return response.error(res, 403, '该信息已被锁定，无法编辑');
    }

    const { itemType, title, category, happenTime, location, featureDesc, imageUrls } = req.body;

    const updateData = {};
    if (title !== undefined) updateData.title = title;
    if (category !== undefined) updateData.category = category;
    if (happenTime !== undefined) {
      // 转换 ISO 日期格式为 MySQL datetime 格式
      try {
        const date = new Date(happenTime);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');
        const second = String(date.getSeconds()).padStart(2, '0');
        updateData.happen_time = `${year}-${month}-${day} ${hour}:${minute}:${second}`;
      } catch (e) {
        console.error('日期格式转换失败:', e);
        updateData.happen_time = happenTime;
      }
    }
    if (location !== undefined) updateData.location = location;
    if (featureDesc !== undefined) updateData.feature_desc = featureDesc;

    // 更新物品信息（会自动设置为 PENDING）
    await Item.update(item.id, updateData);

    // 如果有新图片，更新图片
    if (imageUrls && Array.isArray(imageUrls)) {
      await ItemImage.deleteByItemId(item.id);
      if (imageUrls.length > 0) {
        await ItemImage.createBatch(item.id, imageUrls);
      }
    }

    response.success(res, {
      id: item.id,
      auditStatus: 'PENDING'
    }, '修改成功，已重新提交审核');
  } catch (error) {
    response.error(res, 500, `更新失败：${error.message}`);
  }
};

// 更新业务状态（找回/归还/取消）
exports.updateBizStatus = async (req, res) => {
  try {
    const userId = req.user.id;
    const userRole = req.user.role;
    const { id } = req.params;
    const { bizStatus } = req.body;
    
    if (!bizStatus || !['RESOLVED', 'RETURNED', 'CANCELLED'].includes(bizStatus)) {
      return response.error(res, 400, '业务状态参数错误');
    }

    // 获取物品信息
    const item = await Item.findById(parseInt(id));
    if (!item) {
      return response.error(res, 404, '物品信息不存在');
    }

    // 权限校验：仅本人或管理员可操作
    if (item.user_id !== userId && userRole !== 'ADMIN') {
      return response.error(res, 403, '无权操作该信息');
    }

    // 状态合法性校验
    if (item.item_type === 'LOST' && !['RESOLVED', 'CANCELLED'].includes(bizStatus)) {
      return response.error(res, 400, '寻物信息只能设置为已找回或已取消');
    }
    if (item.item_type === 'FOUND' && !['RETURNED', 'CANCELLED'].includes(bizStatus)) {
      return response.error(res, 400, '拾物信息只能设置为已归还或已取消');
    }

    // 完成态不可回退
    if (!['ONGOING'].includes(item.biz_status)) {
      return response.error(res, 409, '已完成的状态不可更改');
    }

    await Item.updateBizStatus(item.id, bizStatus);

    response.success(res, {
      id: item.id,
      bizStatus
    }, '状态更新成功');
  } catch (error) {
    response.error(res, 500, `状态更新失败：${error.message}`);
  }
};

// 增加浏览次数
exports.addView = async (req, res) => {
  try {
    const { id } = req.params;
    await Item.incrementViewCount(parseInt(id));
    response.success(res, null, '浏览次数已增加');
  } catch (error) {
    response.error(res, 500, `增加浏览次数失败：${error.message}`);
  }
};
