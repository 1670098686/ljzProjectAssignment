// utils/auth.js - 认证工具函数
const TOKEN_KEY = 'accessToken';
const USER_INFO_KEY = 'userInfo';
const PHPSESSID_KEY = 'PHPSESSID';

/**
 * 获取 token
 */
function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

/**
 * 保存 token
 */
function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token);
}

/**
 * 获取 PHPSESSID Cookie
 */
function getPhpSessId() {
  return wx.getStorageSync(PHPSESSID_KEY) || '';
}

/**
 * 保存 PHPSESSID Cookie
 */
function setPhpSessId(phpSessId) {
  if (phpSessId) {
    wx.setStorageSync(PHPSESSID_KEY, phpSessId);
  }
}

/**
 * 从响应头中提取并保存 PHPSESSID
 */
function savePhpSessIdFromHeaders(headers) {
  if (!headers) return;
  
  // 从 Set-Cookie 头中提取 PHPSESSID
  const setCookie = headers['Set-Cookie'] || headers['set-cookie'];
  if (setCookie) {
    if (Array.isArray(setCookie)) {
      for (const cookie of setCookie) {
        const match = cookie.match(/PHPSESSID=([^;]+)/);
        if (match) {
          setPhpSessId(match[1]);
          break;
        }
      }
    } else {
      const match = setCookie.match(/PHPSESSID=([^;]+)/);
      if (match) {
        setPhpSessId(match[1]);
      }
    }
  }
}

/**
 * 清除登录信息
 */
function clearLogin() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_INFO_KEY);
  wx.removeStorageSync(PHPSESSID_KEY);
}

/**
 * 检查是否登录
 */
function checkLogin() {
  return !!getToken();
}

/**
 * 需要登录时跳转
 */
function requireLogin(callback) {
  if (checkLogin()) {
    if (typeof callback === 'function') {
      const result = callback();
      // 如果回调返回Promise，直接返回
      if (result && typeof result.then === 'function') {
        return result;
      }
      // 否则返回一个resolved的Promise
      return Promise.resolve(result);
    }
    return Promise.resolve(true);
  } else {
    // 直接跳转到登录页面
    wx.reLaunch({
      url: '/pages/login/login'
    });
    return Promise.reject(new Error('需要登录'));
  }
}

/**
 * 获取当前用户信息
 */
function getUserInfo() {
  try {
    return JSON.parse(wx.getStorageSync(USER_INFO_KEY) || 'null');
  } catch (e) {
    return null;
  }
}

/**
 * 保存用户信息
 */
function setUserInfo(userInfo) {
  wx.setStorageSync(USER_INFO_KEY, JSON.stringify(userInfo));
}

module.exports = {
  getToken,
  setToken,
  getPhpSessId,
  setPhpSessId,
  savePhpSessIdFromHeaders,
  clearLogin,
  checkLogin,
  requireLogin,
  getUserInfo,
  setUserInfo
};
