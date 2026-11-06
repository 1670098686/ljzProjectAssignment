// pages/report/report.js
const { post } = require('../../utils/request');
const { requireLogin } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

Page({
  data: {
    itemId: null,
    itemTitle: '',
    selectedReasonType: '',
    reasonDetail: '',
    submitting: false
  },

  onLoad(options) {
    requireLogin(() => {
      const itemId = options.id;
      let itemTitle = options.title || '';
      
      try {
        itemTitle = decodeURIComponent(itemTitle);
      } catch (e) {
        console.error('解码失败:', e);
      }
      
      if (!itemId) {
        showToast('缺少物品信息', 'none');
        wx.navigateBack();
        return;
      }

      this.setData({
        itemId: parseInt(itemId),
        itemTitle
      });
    });
  },

  onReasonTypeChange(e) {
    this.setData({
      selectedReasonType: e.detail.value
    });
  },

  onReasonDetailInput(e) {
    this.setData({ reasonDetail: e.detail.value });
  },

  async onSubmit() {
    const { itemId, selectedReasonType, reasonDetail, submitting } = this.data;

    if (submitting) {
      return;
    }

    if (!selectedReasonType) {
      showToast('请选择举报原因', 'none');
      return;
    }

    this.setData({ submitting: true });

    try {
      await post('/reports', {
        itemId,
        reasonType: selectedReasonType,
        reasonDetail
      });
      
      showToast('举报成功', 'success');
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (err) {
      console.error('举报失败:', err);
    } finally {
      this.setData({ submitting: false });
    }
  }
});
