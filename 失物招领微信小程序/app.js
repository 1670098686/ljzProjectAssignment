// app.js
const { checkLogin, getToken, clearLogin, setToken, setUserInfo } = require('./utils/auth');
const { showToast } = require('./utils/util');
const { request } = require('./utils/request');

App({
  globalData: {
    userInfo: null,
    isLoggedIn: false,
    baseUrl: 'http://localhost:3000',
    unreadNotificationCount: 0,
    unreadConversationCount: 0,
    adminPendingItemCount: 0,
    adminPendingReportCount: 0
  },

  onLaunch() {
    // 检查登录状态
    this.checkAuthStatus();
  },

  onShow() {
    // 每次显示时检查登录状态
    this.checkAuthStatus();
    this.loadUnreadCount();
  },

  /**
   * 检查登录状态
   */
  async checkAuthStatus() {
    try {
      const token = getToken();
      
      if (!token) {
        // 没有token，保持未登录状态
        this.globalData.isLoggedIn = false;
        this.globalData.userInfo = null;
        return;
      }
      
      // 有token，调用接口检查登录状态
      const res = await request({
        url: '/auth/check-status',
        method: 'GET'
      });
      
      if (res.data && res.data.isLogin) {
        // 已登录
        this.globalData.isLoggedIn = true;
        // 获取用户详细信息
        this.getUserInfo();
      } else {
        // token无效，清除登录信息
        clearLogin();
        this.globalData.isLoggedIn = false;
        this.globalData.userInfo = null;
      }
    } catch (error) {
      console.error('检查登录状态失败:', error);
      // 出错时清除可能无效的登录信息
      clearLogin();
      this.globalData.isLoggedIn = false;
      this.globalData.userInfo = null;
    }
  },

  /**
   * 执行微信登录
   */
  performWxLogin() {
    wx.login({
      success: (loginRes) => {
        if (loginRes.code) {
          // 调用后端微信登录接口
          this.wxLoginToServer(loginRes.code);
        }
      },
      fail: (err) => {
        console.error('wx.login失败:', err);
      }
    });
  },

  /**
   * 将code发送到服务器进行登录
   */
  async wxLoginToServer(code) {
    try {
      const res = await request({
        url: '/auth/wx-login',
        method: 'POST',
        data: { code }
      });
      
      if (res.data && res.data.accessToken) {
        // 登录成功，保存token和用户信息
        setToken(res.data.accessToken);
        if (res.data.user) {
          setUserInfo(res.data.user);
        }
        this.globalData.isLoggedIn = true;
        this.globalData.userInfo = res.data.user;
        
        console.log('微信登录成功');
      }
    } catch (error) {
      console.error('微信登录失败:', error);
    }
  },

  getUserInfo() {
    request({
      url: '/auth/me',
      method: 'GET'
    }).then(res => {
      this.globalData.userInfo = res.data;
      setUserInfo(res.data);
    }).catch(err => {
      console.error('获取用户信息失败:', err);
      if (err.statusCode === 401) {
        clearLogin();
        this.globalData.isLoggedIn = false;
        this.globalData.userInfo = null;
      }
    });
  },

  logout() {
    clearLogin();
    this.globalData.isLoggedIn = false;
    this.globalData.userInfo = null;
    this.globalData.unreadNotificationCount = 0;
    this.globalData.unreadConversationCount = 0;
    this.globalData.adminPendingItemCount = 0;
    this.globalData.adminPendingReportCount = 0;
    this.updateTabBarBadge();
    wx.reLaunch({
      url: '/pages/login/login'
    });
  },

  loadUnreadCount() {
    if (!this.globalData.isLoggedIn) {
      this.globalData.unreadNotificationCount = 0;
      this.globalData.unreadConversationCount = 0;
      this.globalData.adminPendingItemCount = 0;
      this.globalData.adminPendingReportCount = 0;
      this.updateTabBarBadge();
      return Promise.resolve();
    }

    const requestList = [
      request({
        url: '/messages/notifications/unread-count',
        method: 'GET'
      }),
      request({
        url: '/messages/conversations/unread-count',
        method: 'GET'
      })
    ];

    const isAdminUser = this.globalData.userInfo
      && this.globalData.userInfo.role === 'ADMIN';

    if (isAdminUser) {
      requestList.push(request({
        url: '/admin/reminder-counts',
        method: 'GET'
      }));
    }

    return Promise.allSettled(requestList).then(results => {
      const [notificationResult, conversationResult, adminResult] = results;

      if (notificationResult.status === 'fulfilled') {
        this.globalData.unreadNotificationCount = notificationResult.value.data.count || 0;
      }

      if (conversationResult.status === 'fulfilled') {
        this.globalData.unreadConversationCount = conversationResult.value.data.count || 0;
      }

      if (adminResult && adminResult.status === 'fulfilled') {
        this.globalData.adminPendingItemCount = adminResult.value.data.pendingItemCount || 0;
        this.globalData.adminPendingReportCount = adminResult.value.data.pendingReportCount || 0;
      }

      this.updateTabBarBadge();
    }).catch(err => {
      console.error('获取未读数量失败:', err);
    });
  },

  updateTabBarBadge() {
    try {
      const hasUnread = (this.globalData.unreadNotificationCount || 0) > 0 ||
        (this.globalData.unreadConversationCount || 0) > 0;
      const hasAdminTodo = (this.globalData.adminPendingItemCount || 0) > 0 ||
        (this.globalData.adminPendingReportCount || 0) > 0;

      console.log('更新 TabBar Badge:', { hasUnread, hasAdminTodo, unreadNotificationCount: this.globalData.unreadNotificationCount, unreadConversationCount: this.globalData.unreadConversationCount });

      // 延迟调用，确保 tabBar 已初始化
      setTimeout(() => {
        // 清理数字角标
        try {
          wx.removeTabBarBadge({ index: 2 });
        } catch (e) {}
        try {
          wx.removeTabBarBadge({ index: 3 });
        } catch (e) {}

        // 消息 tab (index 2)
        if (hasUnread) {
          wx.showTabBarRedDot({
            index: 2,
            success: () => console.log('消息 tab 小红点显示成功'),
            fail: (err) => console.log('消息 tab 小红点显示失败:', err)
          });
        } else {
          wx.hideTabBarRedDot({
            index: 2,
            success: () => console.log('消息 tab 小红点隐藏成功'),
            fail: (err) => console.log('消息 tab 小红点隐藏失败:', err)
          });
        }

        // 我的 tab (index 3)
        if (hasAdminTodo) {
          wx.showTabBarRedDot({
            index: 3,
            success: () => console.log('我的 tab 小红点显示成功'),
            fail: (err) => console.log('我的 tab 小红点显示失败:', err)
          });
        } else {
          wx.hideTabBarRedDot({
            index: 3,
            success: () => console.log('我的 tab 小红点隐藏成功'),
            fail: (err) => console.log('我的 tab 小红点隐藏失败:', err)
          });
        }
      }, 100);
    } catch (error) {
      console.error('更新 TabBar Badge 异常:', error);
    }
  }
});
