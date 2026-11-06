// pages/admin/report-list.js
const { get, patch } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast, formatRelativeTime } = require('../../utils/util');

Page({
  data: {
    reports: [],
    loading: false,
    showHandleModal: false,
    currentReport: null,
    actionType: '',
    noteContent: '',
    modalLoading: false
  },
  onLoad() {
    requireLogin(() => {
      const user = getUserInfo();
      if (!user || user.role !== 'ADMIN') {
        wx.showToast({ title: '需要管理员权限', icon: 'none' });
        wx.navigateBack();
        return;
      }
      this.loadReports();
    });
  },
  onShow() {
    this.loadReports();
  },
  loadReports() {
    this.setData({ loading: true });
    get('/admin/reports')
      .then(res => {
        const reports = (res.data.list || []).map(item => {
          return {
            ...item,
            itemTitle: item.item_title || '未知物品',
            reporterName: item.reporter_name || '匿名用户',
            reasonDetail: item.reason,
            itemId: item.item_id,
            reasonType: this.getReasonTypeText(item.reason_type),
            handleResult: item.handle_result,
            createdAt: formatRelativeTime(item.created_at)
          };
        });
        this.setData({ reports });
      })
      .catch(err => {
        console.error('加载举报列表失败:', err);
      })
      .finally(() => {
        this.setData({ loading: false });
        const app = getApp();
        app.loadUnreadCount();
      });
  },
  getReasonTypeText(type) {
    const map = {
      'SCAM': '诈骗',
      'AD': '广告',
      'SENSITIVE': '敏感内容',
      'OTHER': '其他'
    };
    return map[type] || '其他';
  },
  onHandle(e) {
    const id = e.currentTarget.dataset.id;
    wx.showActionSheet({
      itemList: ['忽略举报', '下架物品'],
      success: (res) => {
        const action = res.tapIndex === 0 ? 'IGNORE' : 'REMOVE';
        const report = this.data.reports.find(r => r.id === id);
        this.setData({
          showHandleModal: true,
          currentReport: report,
          actionType: action,
          noteContent: ''
        });
      }
    });
  },
  onNoteInput(e) {
    this.setData({ noteContent: e.detail.value });
  },
  onCancelModal() {
    this.setData({ showHandleModal: false });
  },
  onConfirmHandle() {
    const { currentReport, actionType, noteContent, modalLoading } = this.data;
    if (modalLoading) return;
    if (!currentReport) return;

    this.setData({ modalLoading: true });
    patch(`/admin/reports/${currentReport.id}/handle`, {
      action: actionType,
      note: noteContent || ''
    })
      .then(() => {
        showToast('处理成功', 'success');
        this.setData({ showHandleModal: false });
        this.loadReports();
      })
      .catch(err => {
        console.error('处理失败:', err);
        showToast('处理失败，请重试');
      })
      .finally(() => {
        this.setData({ modalLoading: false });
      });
  },
  goToItem(e) {
    const itemId = e.currentTarget.dataset.id;
    if (itemId) {
      wx.navigateTo({
        url: `/pages/detail/detail?id=${itemId}`
      });
    } else {
      showToast('物品信息缺失', 'none');
    }
  }
});
