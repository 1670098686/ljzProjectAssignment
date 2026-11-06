// pages/admin/audit-detail.js
const { get, patch } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast, showConfirm } = require('../../utils/util');

const rejectReasonOptions = [
  '标题或描述不完整',
  '图片内容不清晰或不相关',
  '包含联系方式违规信息',
  '疑似广告或无关内容',
  '其他（手动输入）'
];

Page({
  data: {
    id: null,
    item: null,
    loading: false
  },
  onLoad(options) {
    requireLogin(() => {
      const user = getUserInfo();
      if (!user || user.role !== 'ADMIN') {
        wx.showToast({ title: '需要管理员权限', icon: 'none' });
        wx.navigateBack();
        return;
      }
      if (options.id) {
        this.setData({ id: options.id });
        this.loadDetail();
      }
    });
  },
  loadDetail() {
    this.setData({ loading: true });
    get(`/admin/items/${this.data.id}`)
      .then(res => {
        this.setData({ item: res.data });
      })
      .catch(err => {
        console.error('加载详情失败:', err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  // 审核通过
  onApprove() {
    showConfirm('确认通过', '确定要审核通过这条信息吗？')
      .then(confirm => {
        if (confirm) {
          patch(`/admin/items/${this.data.id}/audit`, { auditStatus: 'APPROVED' })
            .then(() => {
              showToast('审核通过', 'success');
              this.loadDetail();
            })
            .catch(err => {
              console.error('审核失败:', err);
            });
        }
      });
  },
  // 审核驳回
  onReject() {
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
              this.submitReject(customReason || '内容不符合规范');
            }
          });
          return;
        }

        this.submitReject(rejectReasonOptions[tapIndex]);
      }
    });
  },

  submitReject(reason) {
    patch(`/admin/items/${this.data.id}/audit`, {
      auditStatus: 'REJECTED',
      reason
    })
      .then(() => {
        showToast('已驳回', 'success');
        this.loadDetail();
      })
      .catch(err => {
        console.error('驳回失败:', err);
      });
  },
  // 预览图片
  previewImage(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewImage({
      current: url,
      urls: this.data.item.imageUrls || [url]
    });
  }
});
