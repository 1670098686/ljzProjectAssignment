const { patch, get } = require('../../utils/request');
const { requireLogin } = require('../../utils/auth');
const { showToast, formatRelativeTime } = require('../../utils/util');

Page({
  data: {
    notification: null,
    loading: true
  },

  onLoad(options) {
    const { id } = options;
    if (!id) {
      showToast('通知不存在', 'none');
      wx.navigateBack();
      return;
    }
    
    requireLogin(() => {
      this.loadNotificationDetail(id);
    });
  },

  loadNotificationDetail(id) {
    const app = getApp();
    let notification = null;
    
    if (app.globalData.tempNotifications) {
      notification = app.globalData.tempNotifications.find(item => item.id === parseInt(id));
    }
    
    if (!notification) {
      get(`/messages/notifications/${id}`)
        .then(res => {
          notification = res.data;
          notification.timeText = formatRelativeTime(notification.createdAt);
          
          this.setData({
            notification,
            loading: false
          });

          if (!notification.isRead) {
            this.markAsRead(id);
          }
        })
        .catch(err => {
          console.error('获取通知详情失败:', err);
          showToast('获取通知失败', 'none');
          wx.navigateBack();
        });
    } else {
      notification.timeText = formatRelativeTime(notification.createdAt);
      
      this.setData({
        notification,
        loading: false
      });

      if (!notification.isRead) {
        this.markAsRead(id);
      }
    }
  },

  markAsRead(id) {
    patch('/messages/notifications/read', { notificationIds: [id] })
      .then(() => {
        const app = getApp();
        
        if (app.globalData.tempNotifications) {
          app.globalData.tempNotifications = app.globalData.tempNotifications.map(item => {
            if (item.id === parseInt(id)) {
              return { ...item, isRead: true };
            }
            return item;
          });
        }
        
        if (app.globalData.unreadNotificationCount > 0) {
          app.globalData.unreadNotificationCount = Math.max(0, app.globalData.unreadNotificationCount - 1);
          app.updateTabBarBadge();
        }
      })
      .catch(err => {
        console.error('标记已读失败:', err);
      });
  },

  getNotificationIcon(type) {
    const iconMap = {
      'SYSTEM': '📢',
      'AUDIT': '✅',
      'MESSAGE': '💬'
    };
    return iconMap[type] || '🔔';
  },

  goToRelated() {
    if (this.data.notification && this.data.notification.relatedId) {
      wx.navigateTo({
        url: `/pages/detail/detail?id=${this.data.notification.relatedId}`
      });
    }
  }
});
