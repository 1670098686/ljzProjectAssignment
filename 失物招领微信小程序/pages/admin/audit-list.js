// pages/admin/audit-list.js
const { get, patch } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

const rejectReasonOptions = [
  '标题或描述不完整',
  '图片内容不清晰或不相关',
  '包含联系方式违规信息',
  '疑似广告或无关内容',
  '其他（手动输入）'
];

Page({
  data: {
    activeTab: 'PENDING',
    pendingItems: [],
    auditedItems: [],
    displayedItems: [],
    pendingLoading: false,
    auditedLoading: false,
    loading: false
  },

  onLoad() {
    requireLogin(() => {
      const user = getUserInfo();
      if (!user || user.role !== 'ADMIN') {
        wx.showToast({ title: '需要管理员权限', icon: 'none' });
        wx.navigateBack();
        return;
      }
      this.loadAuditLists();
    });
  },

  loadAuditLists() {
    this.setData({ loading: true, pendingLoading: true, auditedLoading: true });

    Promise.all([
      get('/admin/items/pending'),
      get('/admin/items/audited')
    ])
      .then(([pendingRes, auditedRes]) => {
        const pendingItems = pendingRes.data.list || [];
        const auditedItems = auditedRes.data.list || [];

        this.setData({
          pendingItems,
          auditedItems,
          displayedItems: this.data.activeTab === 'PENDING' ? pendingItems : auditedItems
        });
      })
      .catch(err => {
        console.error('加载审核列表失败:', err);
      })
      .finally(() => {
        this.setData({
          loading: false,
          pendingLoading: false,
          auditedLoading: false
        });

        const app = getApp();
        app.loadUnreadCount();
      });
  },

  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      activeTab: tab,
      displayedItems: tab === 'PENDING' ? this.data.pendingItems : this.data.auditedItems
    });
  },

  onApprove(e) {
    const id = e.currentTarget.dataset.id;
    patch(`/admin/items/${id}/audit`, { auditStatus: 'APPROVED' })
      .then(() => {
        showToast('审核通过', 'success');
        this.loadAuditLists();
      })
      .catch(err => {
        console.error('审核失败:', err);
      });
  },

  onReject(e) {
    const id = e.currentTarget.dataset.id;
    wx.showActionSheet({
      itemList: rejectReasonOptions,
      success: (actionRes) => {
        const tapIndex = actionRes.tapIndex;
        if (tapIndex === rejectReasonOptions.length - 1) {
          wx.showModal({
            title: '审核驳回',
            editable: true,
            placeholderText: '请输入驳回原因（可选）',
            success: (modalRes) => {
              if (!modalRes.confirm) return;
              const customReason = (modalRes.content || '').trim();
              this.submitReject(id, customReason || '内容不符合规范');
            }
          });
          return;
        }

        this.submitReject(id, rejectReasonOptions[tapIndex]);
      }
    });
  },

  submitReject(id, reason) {
    patch(`/admin/items/${id}/audit`, { auditStatus: 'REJECTED', reason })
      .then(() => {
        showToast('已驳回', 'success');
        this.loadAuditLists();
      })
      .catch(err => {
        console.error('驳回失败:', err);
      });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/admin/audit-detail?id=${id}`
    });
  },

  stopPropagation() {
  },

  previewImage(e) {
    const urls = e.currentTarget.dataset.urls;
    wx.previewImage({
      current: urls[0],
      urls: urls
    });
  }
});
