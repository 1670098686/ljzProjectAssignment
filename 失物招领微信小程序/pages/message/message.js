// pages/message/message.js
const { get, patch } = require('../../utils/request');
const { requireLogin } = require('../../utils/auth');
const { formatRelativeTime, showToast } = require('../../utils/util');
const { formatImageUrl } = require('../../utils/imageUrl');

Page({
  data: {
    conversations: [],
    notifications: [],
    loading: false,
    activeTab: 'conversations',
    notificationsLoading: false,
    hasMoreNotifications: true,
    notificationPage: 1,
    unreadNotificationCount: 0,
    unreadConversationCount: 0
  },

  onLoad() {
    requireLogin(() => {
      this.loadConversations();
      this.loadUnreadCount();
    });
  },

  onShow() {
    if (this.data.activeTab === 'conversations') {
      requireLogin(() => {
        // 完全刷新会话列表 + 两个 tab 的未读数
        this.loadConversations();
        this.loadUnreadCount();
      });
    } else if (this.data.activeTab === 'notifications') {
      requireLogin(() => {
        // 在通知 tab 下，除了刷新通知，也需要从后端拉取会话未读数
        // 因为用户可能刚从聊天页面返回，会话未读状态已改变
        this.loadNotifications(true);
        this.loadConversationUnreadCount();
      });
    }
  },

  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });
    
    // 每次切换标签都重新加载，确保实时性
    if (tab === 'notifications') {
      this.loadNotifications(true);
      // 切换到通知 tab 时，也刷新会话未读数（保持聊天会话 tab 的角标准确）
      this.loadConversationUnreadCount();
    } else {
      requireLogin(() => {
        this.loadConversations();
        // 同时更新通知未读数量，确保"系统通知"标签 badge 与实际数据一致
        this.loadUnreadCount();
      });
    }
  },

  onReachBottom() {
    if (this.data.activeTab === 'notifications' && this.data.hasMoreNotifications) {
      this.loadNotifications(false);
    }
  },

  onPullDownRefresh() {
    if (this.data.activeTab === 'notifications') {
      this.loadNotifications(true);
      // 下拉刷新时也同步会话未读数
      this.loadConversationUnreadCount().then(() => {
        wx.stopPullDownRefresh();
      });
    } else {
      this.loadConversations().then(() => {
        wx.stopPullDownRefresh();
      });
    }
  },

  loadConversations() {
    return requireLogin(() => {
      this.setData({ loading: true });

      return get('/messages/conversations')
        .then(res => {
          const list = res.data.list || [];
          console.log('📋 后端返回的会话数据:', list); // 调试日志
          const unreadConversationCount = list.reduce((sum, item) => sum + (item.unreadCount || 0), 0);

          const app = getApp();
          app.globalData.unreadConversationCount = unreadConversationCount;
          app.updateTabBarBadge();

          this.setData({
            conversations: list.map(item => {
              const nickname = item.otherUser?.nickname || '用户';
              const avatarChar = nickname.charAt(0) || '用';
              const mainTitle = item.itemTitle ? item.itemTitle : `与${nickname}的会话`;
              const subDesc = item.itemTitle 
                ? `${nickname}：${item.lastMessage || '暂无消息'}`
                : item.lastMessage || '暂无消息';
              return {
                ...item,
                otherUser: {
                  ...item.otherUser,
                  avatarChar: avatarChar
                },
                lastMessageTimeText: formatRelativeTime(item.lastMessageAt),
                title: mainTitle,
                subDesc: subDesc
              };
            }),
            unreadConversationCount: unreadConversationCount
          });
          console.log('✨ 处理后的会话数据:', this.data.conversations); // 调试日志
        })
        .catch(err => {
          console.error('加载会话失败:', err);
        })
        .finally(() => {
          this.setData({ loading: false });
        });
    });
  },

  loadNotifications(refresh = false) {
    return requireLogin(() => {
      const page = refresh ? 1 : this.data.notificationPage;
      
      if (refresh) {
        this.setData({ notifications: [], notificationPage: 1, hasMoreNotifications: true });
      }

      this.setData({ notificationsLoading: true });

      return get('/messages/notifications', { page, pageSize: 20 })
        .then(res => {
          const list = res.data.list || [];
          const total = res.data.total || 0;
          const totalPages = res.data.totalPages || 1;
          
          const formattedList = list.map(item => ({
            ...item,
            icon: this.getNotificationIcon(item.type, item),
            timeText: formatRelativeTime(item.createdAt),
            isRead: item.isRead,
            shortContent: this.getShortContent(item.content)
          }));

          const allNotifications = refresh ? formattedList : [...this.data.notifications, ...formattedList];
          
          const app = getApp();
          app.globalData.tempNotifications = allNotifications;

          // 1. 从当前列表计算未读数量，确保标签 badge 与列表红点立即同步
          const listUnreadCount = allNotifications.filter(item => !item.isRead).length;
          
          this.setData({
            notifications: allNotifications,
            notificationPage: page + 1,
            hasMoreNotifications: page < totalPages,
            unreadNotificationCount: listUnreadCount
          });

          // 2. 同步更新全局和 tabBar badge
          app.globalData.unreadNotificationCount = listUnreadCount;
          app.updateTabBarBadge();

          // 3. 分页场景下，同时从后端获取完整未读数量（确保有未读通知在后续页时也能显示准确数字）
          if (page === 1) {
            get('/messages/notifications/unread-count').then(unreadRes => {
              const serverCount = unreadRes.data?.count || 0;
              // 取较大值：后端完整数量 vs 当前页列表中的未读数量
              // 如果后端返回更大，说明有未读通知在后续页面（分页加载时会逐步显示）
              if (serverCount > listUnreadCount) {
                this.setData({ unreadNotificationCount: serverCount });
                const app2 = getApp();
                app2.globalData.unreadNotificationCount = serverCount;
                app2.updateTabBarBadge();
              }
            }).catch(err => {
              console.error('获取未读数量失败:', err);
            });
          }
        })
        .catch(err => {
          console.error('加载通知失败:', err);
        })
        .finally(() => {
          this.setData({ notificationsLoading: false });
        });
    });
  },

  getNotificationIcon(type, item) {
    // 先按类型判断，然后再根据内容细化
    if (type === 'AUDIT' || type === 'SYSTEM') {
      const title = item.title || '';
      const content = item.content || '';
      if (title.includes('驳回') || content.includes('驳回')) {
        return '❌';
      }
      if (title.includes('通过') || content.includes('通过')) {
        return '✅';
      }
    }
    
    const iconMap = {
      'SYSTEM': '📢',
      'AUDIT': '📋',
      'MESSAGE': '💬'
    };
    return iconMap[type] || '🔔';
  },

  loadUnreadCount() {
    return requireLogin(() => {
      return get('/messages/notifications/unread-count')
        .then(res => {
          const count = res.data.count || 0;
          this.setData({
            unreadNotificationCount: count
          });

          const app = getApp();
          app.globalData.unreadNotificationCount = count;
          app.updateTabBarBadge();
        })
        .catch(err => {
          console.error('获取未读数量失败:', err);
        });
    });
  },

  // 从后端拉取会话未读总数（用于 notifations tab 下，轻量刷新会话 tab 角标）
  loadConversationUnreadCount() {
    return requireLogin(() => {
      return get('/messages/conversations/unread-count')
        .then(res => {
          const count = res.data.count || 0;
          this.setData({
            unreadConversationCount: count
          });

          const app = getApp();
          app.globalData.unreadConversationCount = count;
          app.updateTabBarBadge();
        })
        .catch(err => {
          console.error('获取会话未读数量失败:', err);
        });
    });
  },

  getShortContent(content) {
    if (!content) return '';
    const maxLength = 50;
    return content.length > maxLength ? content.substring(0, maxLength) + '...' : content;
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    
    const notification = this.data.notifications.find(item => item.id === id);
    if (notification && !notification.isRead) {
      this.markAsRead(e);
    }
    
    wx.navigateTo({
      url: `/pages/notification-detail/notification-detail?id=${id}`
    });
  },

  markAsRead(e) {
    const id = e.currentTarget.dataset.id;
    const notification = this.data.notifications.find(item => item.id === id);
    
    if (!notification || notification.isRead) {
      return;
    }
    
    patch('/messages/notifications/read', { notificationIds: [id] })
      .then(() => {
        const notifications = this.data.notifications.map(item => {
          if (item.id === id) {
            return { ...item, isRead: true };
          }
          return item;
        });
        const newUnreadCount = Math.max(0, this.data.unreadNotificationCount - 1);
        this.setData({ notifications, unreadNotificationCount: newUnreadCount });
        
        const app = getApp();
        app.globalData.unreadNotificationCount = newUnreadCount;
        app.updateTabBarBadge();
      })
      .catch(err => {
        console.error('标记已读失败:', err);
      });
  },

  goToChat(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/chat/chat?id=${id}`
    });
  }
});
