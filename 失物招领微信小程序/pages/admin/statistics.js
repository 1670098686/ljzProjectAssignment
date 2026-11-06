// pages/admin/statistics.js
const { get } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

Page({
  data: {
    stats: null,
    loading: false
  },

  onLoad() {
    requireLogin(() => {
      const user = getUserInfo();
      if (!user || user.role !== 'ADMIN') {
        showToast('需要管理员权限', 'none');
        wx.navigateBack();
        return;
      }
      this.loadStatistics();
    });
  },

  loadStatistics() {
    this.setData({ loading: true });
    get('/admin/statistics')
      .then(res => {
        this.setData({ stats: res.data });
      })
      .catch(err => {
        console.error('加载统计数据失败:', err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  goToStatDetail(e) {
    const type = e.currentTarget.dataset.type;
    console.log('点击统计卡片，类型：', type);
    wx.navigateTo({
      url: `/pages/admin/stat-detail?type=${type}`
    });
  }
});
