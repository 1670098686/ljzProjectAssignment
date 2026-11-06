// pages/admin/stat-detail.js
const { get } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

Page({
  data: {
    type: '',
    pageTitle: '',
    pageSubtitle: '',
    pageIcon: '',
    overviewValue: '',
    overviewLabel: '',
    trendData: [],
    listData: [],
    loading: true
  },

  onLoad(options) {
    requireLogin(() => {
      const user = getUserInfo();
      if (!user || user.role !== 'ADMIN') {
        showToast('需要管理员权限', 'none');
        wx.navigateBack();
        return;
      }
      const type = options.type || 'total';
      this.setData({ type });
      this.loadData(type);
    });
  },

  loadData(type) {
    const config = this.getTypeConfig(type);
    this.setData({
      ...config,
      loading: true
    });

    setTimeout(() => {
      const mockData = this.getMockData(type);
      this.setData({
        ...mockData,
        loading: false
      });
    }, 300);
  },

  getTypeConfig(type) {
    const configs = {
      total: {
        pageTitle: '总信息数',
        pageSubtitle: '平台所有失物招领信息',
        pageIcon: '📊',
        overviewLabel: '累计发布'
      },
      lost: {
        pageTitle: '寻物信息',
        pageSubtitle: '所有失物求助信息',
        pageIcon: '🔍',
        overviewLabel: '寻物启事'
      },
      found: {
        pageTitle: '拾物信息',
        pageSubtitle: '所有捡拾物品信息',
        pageIcon: '📦',
        overviewLabel: '拾物招领'
      },
      approved: {
        pageTitle: '审核通过率',
        pageSubtitle: '信息审核统计',
        pageIcon: '✓',
        overviewLabel: '通过比率'
      },
      resolved: {
        pageTitle: '成功解决',
        pageSubtitle: '已找回或已归还',
        pageIcon: '🎯',
        overviewLabel: '已解决'
      },
      report: {
        pageTitle: '举报处理率',
        pageSubtitle: '举报投诉处理统计',
        pageIcon: '⚠️',
        overviewLabel: '处理比率'
      }
    };
    return configs[type] || configs.total;
  },

  getMockData(type) {
    const mockItems = [
      { id: 1, title: '校园卡', type: '寻物', typeClass: 'lost', status: '已通过', statusClass: 'approved', time: '2026-05-03' },
      { id: 2, title: '蓝色雨伞', type: '拾物', typeClass: 'found', status: '待审核', statusClass: 'pending', time: '2026-05-02' },
      { id: 3, title: '黑色双肩包', type: '寻物', typeClass: 'lost', status: '已驳回', statusClass: 'rejected', time: '2026-05-01' },
      { id: 4, title: 'AirPods Pro耳机', type: '拾物', typeClass: 'found', status: '已通过', statusClass: 'approved', time: '2026-04-29' },
      { id: 5, title: '英语四六级词汇书', type: '寻物', typeClass: 'lost', status: '待审核', statusClass: 'pending', time: '2026-04-30' }
    ];

    const trendData = [
      { label: '周一', value: 12, percentage: 60 },
      { label: '周二', value: 18, percentage: 90 },
      { label: '周三', value: 10, percentage: 50 },
      { label: '周四', value: 20, percentage: 100 },
      { label: '周五', value: 15, percentage: 75 },
      { label: '周六', value: 8, percentage: 40 },
      { label: '周日', value: 14, percentage: 70 }
    ];

    const values = {
      total: { overviewValue: '156', listData: mockItems, trendData },
      lost: { overviewValue: '89', listData: mockItems.filter(i => i.type === '寻物'), trendData },
      found: { overviewValue: '67', listData: mockItems.filter(i => i.type === '拾物'), trendData },
      approved: { overviewValue: '92%', listData: mockItems, trendData },
      resolved: { overviewValue: '45', listData: mockItems.filter(i => i.status === '已通过'), trendData },
      report: { overviewValue: '85%', listData: mockItems, trendData }
    };

    return values[type] || values.total;
  },

  goToItem(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/admin/audit-detail?id=${id}`
    });
  }
});