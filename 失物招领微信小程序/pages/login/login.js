// pages/login/login.js
const { post } = require('../../utils/request');
const { setToken, setUserInfo, checkLogin } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

Page({
  data: {
    loginMode: 'wx', // wx: 微信登录, password: 密码登录
    email: '',
    password: '',
    loading: false
  },

  onLoad() {
    // 如果已登录，跳转首页
    if (checkLogin()) {
      wx.switchTab({
        url: '/pages/index/index'
      });
    }
  },

  // 切换登录模式
  switchMode(e) {
    this.setData({
      loginMode: e.currentTarget.dataset.mode
    });
  },

  // 邮箱输入
  onEmailInput(e) {
    this.setData({ email: e.detail.value });
  },

  // 密码输入
  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  // 微信登录
  onWxLogin() {
    const that = this;
    
    if (that.data.loading) return;
    that.setData({ loading: true });

    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          showToast('微信登录失败');
          that.setData({ loading: false });
          return;
        }

        // 获取用户信息授权
        wx.getUserProfile({
          desc: '用于完善个人资料',
          success(profileRes) {
            // 调用后端登录接口
            post('/auth/wx-login', {
              code: loginRes.code,
              nickName: profileRes.userInfo.nickName,
              avatarUrl: profileRes.userInfo.avatarUrl
            }).then(res => {
              const { accessToken, user } = res.data || {};
              
              if (!accessToken || !user) {
                showToast('登录数据异常');
                that.setData({ loading: false });
                return;
              }
              
              // 演示模式：直接登录成功
              setToken(accessToken);
              setUserInfo(user);
              showToast(user.role === 'ADMIN' ? '管理员登录成功' : '登录成功', 'success');
              
              setTimeout(() => {
                wx.switchTab({
                  url: '/pages/index/index'
                });
              }, 1500);
            }).catch(err => {
              console.error('微信登录失败:', err);
            }).finally(() => {
              that.setData({ loading: false });
            });
          },
          fail() {
            // 用户取消授权，使用默认信息登录
            post('/auth/wx-login', {
              code: loginRes.code
            }).then(res => {
              const { accessToken, user } = res.data || {};
              
              if (!accessToken || !user) {
                showToast('登录数据异常');
                that.setData({ loading: false });
                return;
              }
              
              // 演示模式：直接登录成功
              setToken(accessToken);
              setUserInfo(user);
              showToast(user.role === 'ADMIN' ? '管理员登录成功' : '登录成功', 'success');
              
              setTimeout(() => {
                wx.switchTab({
                  url: '/pages/index/index'
                });
              }, 1500);
            }).catch(err => {
              console.error('微信登录失败:', err);
            }).finally(() => {
              that.setData({ loading: false });
            });
          }
        });
      },
      fail() {
        showToast('微信登录失败');
        that.setData({ loading: false });
      }
    });
  },

  // 密码登录
  onPasswordLogin() {
    const { email, password, loading } = this.data;
    
    if (loading) return;
    
    if (!email) {
      showToast('请输入邮箱');
      return;
    }
    
    if (!password) {
      showToast('请输入密码');
      return;
    }

    this.setData({ loading: true });

    post('/auth/password-login', { email, password })
      .then(res => {
        const { accessToken, user } = res.data || {};
        
        if (!accessToken || !user) {
          showToast('登录数据异常');
          this.setData({ loading: false });
          return;
        }
        
        setToken(accessToken);
        setUserInfo(user);
        showToast('登录成功', 'success');
        
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index'
          });
        }, 1500);
      })
      .catch(err => {
        console.error('密码登录失败:', err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  // 去注册
  goToRegister() {
    wx.navigateTo({
      url: '/pages/register/register'
    });
  }
});
