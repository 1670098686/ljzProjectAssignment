// pages/admin/send-notification.js
const { post, get } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

Page({
  data: {
    title: '',
    content: '',
    sendToAll: true,
    targetUserIds: [],
    users: [],
    loading: false,
    submitting: false
  },

  onLoad() {
    requireLogin(() => {
      this.checkAdmin();
    });
  },

  checkAdmin() {
    const user = getUserInfo();
    if (!user || user.role !== 'ADMIN') {
      showToast('需要管理员权限', 'none');
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  onSendToAllChange(e) {
    this.setData({ sendToAll: e.detail.value });
  },

  async onSubmit() {
    const { title, content, sendToAll, targetUserIds } = this.data;

    if (!title || !content) {
      showToast('请填写标题和内容', 'none');
      return;
    }

    if (title.length > 128) {
      showToast('标题不能超过128个字符', 'none');
      return;
    }

    if (!sendToAll && targetUserIds.length === 0) {
      showToast('请选择接收通知的用户', 'none');
      return;
    }

    this.setData({ submitting: true });

    try {
      const res = await post('/admin/notifications/send', {
        title,
        content,
        sendToAll,
        targetUserIds: sendToAll ? undefined : targetUserIds
      });

      showToast('发送成功', 'success');
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (err) {
      console.error('发送通知失败:', err);
      showToast(err.message || '发送通知失败', 'none');
    } finally {
      this.setData({ submitting: false });
    }
  }
});
