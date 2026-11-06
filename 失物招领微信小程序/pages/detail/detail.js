// pages/detail/detail.js
const { get, patch } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast, showConfirm, getItemTypeText, getAuditStatusText, getAuditStatusColor, getBizStatusText, getBizStatusColor, formatDateTime } = require('../../utils/util');
const { formatImageUrl } = require('../../utils/imageUrl');

Page({
  data: {
    item: null,
    loading: true,
    isOwner: false,
    isAdmin: false
  },

  onLoad(options) {
    requireLogin(() => {
      const { id } = options;
      if (id) {
        this.loadDetail(id);
      }
    });
  },

  loadDetail(id) {
    this.setData({ loading: true });

    const user = getUserInfo();
    const detailUrl = user && user.role === 'ADMIN' ? `/admin/items/${id}` : `/items/${id}`;

    get(detailUrl)
      .then(res => {
        const item = res.data;
        const currentUser = getUserInfo();
        
        // 格式化图片 URL
        if (item.imageUrls && Array.isArray(item.imageUrls)) {
          item.imageUrls = item.imageUrls.map(url => formatImageUrl(url));
        }
        
        this.setData({
          item: {
            ...item,
            itemTypeText: getItemTypeText(item.itemType),
            auditStatusText: getAuditStatusText(item.auditStatus),
            auditStatusColor: getAuditStatusColor(item.auditStatus),
            bizStatusText: getBizStatusText(item.bizStatus, item.itemType, item.auditStatus),
            bizStatusColor: getBizStatusColor(item.bizStatus, item.auditStatus),
            happenTimeText: formatDateTime(item.happenTime),
            publishedAtText: formatDateTime(item.publishedAt)
          },
          isOwner: currentUser && currentUser.id === item.userId,
          isAdmin: currentUser && currentUser.role === 'ADMIN'
        });
      })
      .catch(err => {
        console.error('加载详情失败：', err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  // 创建会话
  onStartChat() {
    requireLogin(() => {
      const { post } = require('../../utils/request');
      const user = getUserInfo();
      
      post('/messages/conversations', {
        targetUserId: this.data.item.userId,
        itemId: this.data.item.id
      }).then(res => {
        wx.navigateTo({
          url: `/pages/chat/chat?id=${res.data.conversationId}`
        });
      }).catch(err => {
        console.error('创建会话失败:', err);
      });
    });
  },

  // 举报
  onReport() {
    requireLogin(() => {
      const item = this.data.item;
      if (item) {
        wx.navigateTo({
          url: `/pages/report/report?id=${item.id}&title=${encodeURIComponent(item.title)}`
        });
      }
    });
  },

  // 编辑
  onEdit() {
    console.log('点击编辑按钮');
    console.log('物品ID:', this.data.item.id);
    console.log('是否所有者:', this.data.isOwner);
    console.log('是否管理员:', this.data.isAdmin);
    console.log('物品状态:', this.data.item);
    
    if (!this.data.item || !this.data.item.id) {
      showToast('物品信息加载中，请稍候');
      return;
    }
    
    // 因为publish是tabbar页面，使用全局变量传递编辑信息
    const app = getApp();
    app.globalData.editItemId = this.data.item.id;
    
    wx.switchTab({
      url: '/pages/publish/publish',
      fail: (err) => {
        console.error('跳转失败:', err);
        showToast('跳转失败，请重试');
      }
    });
  },

  // 更新业务状态
  onUpdateStatus(e) {
    const bizStatus = e.currentTarget.dataset.status;
    const statusText = getBizStatusText(bizStatus);
    
    showConfirm('确认状态', `确定标记为「${statusText}」吗？`)
      .then(confirm => {
        if (confirm) {
          patch(`/items/${this.data.item.id}/biz-status`, { bizStatus })
            .then(() => {
              showToast('状态更新成功', 'success');
              this.loadDetail(this.data.item.id);
            })
            .catch(err => {
              console.error('状态更新失败:', err);
            });
        }
      });
  },

  // 预览图片
  onPreviewImage(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewImage({
      current: url,
      urls: this.data.item.imageUrls
    });
  }
});
