// pages/profile/profile.js
const { get } = require('../../utils/request');
const { requireLogin, getUserInfo, clearLogin } = require('../../utils/auth');
const { showToast, showConfirm, formatRelativeTime, getBizStatusText } = require('../../utils/util');

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    isAdmin: false,
    myItems: [],
    myItemsLoading: false,
    totalCount: 0,
    approvedCount: 0,
    pendingCount: 0,
    adminPendingItemCount: 0,
    adminPendingReportCount: 0,
    avatarChar: '用'
  },

  onShow() {
    requireLogin(() => {
      this.loadUserInfo();
      // 刷新 tabBar 小红点
      const app = getApp();
      if (app && app.loadUnreadCount) {
        app.loadUnreadCount();
      }
    });
  },

  loadUserInfo() {
    const user = getUserInfo();
    if (user) {
      let avatarCharVal = '用';
      if (user.nickName) {
        avatarCharVal = user.nickName.charAt(0);
      }
      this.setData({
        isLoggedIn: true,
        userInfo: user,
        isAdmin: user.role === 'ADMIN',
        avatarChar: avatarCharVal
      });
      this.loadMyItems();
      if (this.data.isAdmin) {
        this.syncAdminReminders();
      }
    } else {
      this.setData({
        isLoggedIn: false,
        userInfo: null
      });
    }
  },

  loadMyItems() {
    this.setData({ myItemsLoading: true });
    get('/auth/me/items', { pageSize: 100 })
      .then(res => {
        const list = res.data.list || [];
        const totalCount = res.data.total || 0;
        const items = [];
        for (let i = 0; i < list.length; i++) {
          const item = list[i];
          items.push({
            id: item.id,
            title: item.title,
            itemType: item.itemType,
            auditStatus: item.auditStatus,
            bizStatus: item.bizStatus,
            publishedAt: item.publishedAt,
            publishedAtText: formatRelativeTime(item.publishedAt),
            statusText: getBizStatusText(item.bizStatus, item.itemType, item.auditStatus)
          });
        }
        
        let approvedCount = 0;
        let pendingCount = 0;
        for (let j = 0; j < items.length; j++) {
          if (items[j].auditStatus === 'APPROVED') {
            approvedCount++;
          } else if (items[j].auditStatus === 'PENDING') {
            pendingCount++;
          }
        }
        this.setData({
          myItems: items,
          totalCount: totalCount,
          approvedCount: approvedCount,
          pendingCount: pendingCount
        });
      })
      .catch(err => {
        console.error('加载我的发布失败:', err);
      })
      .finally(() => {
        this.setData({ myItemsLoading: false });
      });
  },

  syncAdminReminders() {
    const app = getApp();
    app.loadUnreadCount().then(() => {
      this.setData({
        adminPendingItemCount: app.globalData.adminPendingItemCount || 0,
        adminPendingReportCount: app.globalData.adminPendingReportCount || 0
      });
    });
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  logout() {
    showConfirm('退出登录', '确定要退出登录吗？')
      .then(confirm => {
        if (confirm) {
          clearLogin();
          this.setData({
            isLoggedIn: false,
            userInfo: null,
            isAdmin: false,
            myItems: []
          });
          showToast('已退出登录', 'success');
          
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/login/login'
            });
          }, 1000);
        }
      });
  },

  goToMyItems(e) {
    const status = e.currentTarget.dataset.status;
    wx.navigateTo({
      url: '/pages/profile/my-items?status=' + status
    });
  },

  goToPublish() {
    wx.switchTab({
      url: '/pages/publish/publish'
    });
  },

  goToAbout() {
    showToast('功能开发中');
  },

  goToHelp() {
    showToast('功能开发中');
  },

  goToAdminAudit() {
    wx.navigateTo({
      url: '/pages/admin/audit-list'
    });
  },

  goToAdminReports() {
    wx.navigateTo({
      url: '/pages/admin/report-list'
    });
  },

  goToAdminStatistics() {
    wx.navigateTo({
      url: '/pages/admin/statistics'
    });
  },

  goToSendNotification() {
    wx.navigateTo({
      url: '/pages/admin/send-notification'
    });
  },

  goToMyItemDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/detail/detail?id=' + id
    });
  }
});
