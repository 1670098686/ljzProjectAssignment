// pages/register/register.js
const { post } = require('../../utils/request');
const { setToken, setUserInfo, checkLogin } = require('../../utils/auth');
const { showToast } = require('../../utils/util');

Page({
  data: {
    nickname: '',
    email: '',
    code: '',
    password: '',
    confirmPassword: '',
    loading: false,
    sendCodeLoading: false,
    canSendCode: true,
    countdown: 0
  },

  onLoad() {
    if (checkLogin()) {
      wx.navigateBack();
    }
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  onEmailInput(e) {
    this.setData({ email: e.detail.value });
  },

  onCodeInput(e) {
    this.setData({ code: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  onConfirmPasswordInput(e) {
    this.setData({ confirmPassword: e.detail.value });
  },

  // 发送验证码
  onSendCode() {
    const { email, canSendCode, countdown } = this.data;

    if (!canSendCode || countdown > 0) return;

    if (!email) {
      showToast('请输入邮箱');
      return;
    }

    // 简单邮箱格式校验
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showToast('邮箱格式不正确');
      return;
    }

    this.setData({ canSendCode: false });

    post('/auth/email-code/send', { email, purpose: 'REGISTER' })
      .then(res => {
        const demoCode = res.data.demoCode || '123456';
        showToast(`验证码已发送：${demoCode}`, 'success');
        // 自动填充验证码
        this.setData({ code: demoCode });
        // 开始倒计时
        this.startCountdown();
      })
      .catch(err => {
        console.error('发送验证码失败:', err);
        this.setData({ canSendCode: true });
      });
  },

  startCountdown() {
    let count = 60;
    this.setData({ countdown: count });

    const timer = setInterval(() => {
      count--;
      if (count <= 0) {
        clearInterval(timer);
        this.setData({ countdown: 0, canSendCode: true });
      } else {
        this.setData({ countdown: count });
      }
    }, 1000);
  },

  // 注册
  onRegister() {
    const { nickname, email, code, password, confirmPassword, loading } = this.data;

    if (loading) return;

    if (!nickname) {
      showToast('请输入昵称');
      return;
    }

    if (nickname.length < 2) {
      showToast('昵称至少2个字符');
      return;
    }

    if (!email) {
      showToast('请输入邮箱');
      return;
    }

    if (!code) {
      showToast('请输入验证码');
      return;
    }

    if (!password) {
      showToast('请输入密码');
      return;
    }

    if (password.length < 6) {
      showToast('密码长度不能少于 6 位');
      return;
    }

    if (password !== confirmPassword) {
      showToast('两次输入的密码不一致');
      return;
    }

    this.setData({ loading: true });
    const that = this;

    // 获取微信 code
    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          showToast('获取微信授权失败');
          that.setData({ loading: false });
          return;
        }

        post('/auth/register', {
          nickname,
          email,
          emailCode: code,
          password,
          wxCode: loginRes.code
        })
          .then(res => {
            showToast('注册成功，正在登录...', 'success');
            
            // 注册成功后直接用密码登录
            post('/auth/password-login', { email, password })
              .then(loginRes => {
                const { accessToken, user } = loginRes.data || {};
                if (!accessToken || !user) {
                  showToast('登录数据异常');
                  setTimeout(() => {
                    wx.redirectTo({
                      url: '/pages/login/login'
                    });
                  }, 1500);
                  return;
                }
                setToken(accessToken);
                setUserInfo(user);
                showToast(user.role === 'ADMIN' ? '管理员注册成功' : '注册成功', 'success');
                
                setTimeout(() => {
                  wx.switchTab({
                    url: '/pages/index/index'
                  });
                }, 1500);
              })
              .catch(loginErr => {
                console.error('登录失败:', loginErr);
                setTimeout(() => {
                  wx.redirectTo({
                    url: '/pages/login/login'
                  });
                }, 1500);
              });
          })
          .catch(err => {
            console.error('注册失败:', err);
          })
          .finally(() => {
            that.setData({ loading: false });
          });
      },
      fail() {
        showToast('获取微信授权失败');
        that.setData({ loading: false });
      }
    });
  },

  // 跳转登录
  goToLogin() {
    wx.redirectTo({
      url: '/pages/login/login'
    });
  }
});
