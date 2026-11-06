// utils/request.js - 统一请求封装
const { getToken, getPhpSessId, savePhpSessIdFromHeaders } = require('./auth');

// 后端基础 URL
// 开发环境使用本地地址，生产环境替换为实际后端地址
const BASE_URL = 'http://localhost:3000';

/**
 * 构建 Cookie 头
 */
function buildCookieHeader() {
  const phpSessId = getPhpSessId();
  if (phpSessId) {
    return `PHPSESSID=${phpSessId}`;
  }
  return '';
}

/**
 * 统一请求方法
 */
function request(options) {
  const { url, method = 'GET', data = {}, header = {} } = options;
  const token = getToken();
  const cookieHeader = buildCookieHeader();

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}/api/v1${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'Cookie': cookieHeader,
        ...header
      },
      success(res) {
        const { statusCode, data: resData, header: responseHeaders } = res;
        
        // 从响应头中保存 PHPSESSID
        savePhpSessIdFromHeaders(responseHeaders);
        
        if (statusCode === 200) {
          resolve(resData);
        } else if (statusCode === 401) {
          wx.removeStorageSync('accessToken');
          wx.showToast({
            title: '登录已过期',
            icon: 'none'
          });
          setTimeout(() => {
            wx.navigateTo({
              url: '/pages/login/login'
            });
          }, 1500);
          reject(new Error('登录已过期'));
        } else if (statusCode === 403) {
          wx.showToast({
            title: '权限不足',
            icon: 'none'
          });
          reject(new Error(resData.message || '权限不足'));
        } else if (statusCode === 404) {
          wx.showToast({
            title: '资源不存在',
            icon: 'none'
          });
          reject(new Error(resData.message || '资源不存在'));
        } else {
          const message = resData.message || '请求失败';
          wx.showToast({
            title: message,
            icon: 'none'
          });
          reject(new Error(message));
        }
      },
      fail(err) {
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
}

/**
 * GET 请求
 */
function get(url, data) {
  return request({ url, method: 'GET', data });
}

/**
 * POST 请求
 */
function post(url, data) {
  return request({ url, method: 'POST', data });
}

/**
 * PUT 请求
 */
function put(url, data) {
  return request({ url, method: 'PUT', data });
}

/**
 * PATCH 请求
 */
function patch(url, data) {
  return request({ url, method: 'PATCH', data });
}

/**
 * DELETE 请求
 */
function del(url, data) {
  return request({ url, method: 'DELETE', data });
}

/**
 * 上传图片
 */
function uploadImage(filePath) {
  const token = getToken();
  const cookieHeader = buildCookieHeader();
  
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${BASE_URL}/api/v1/files/images`,
      filePath,
      name: 'file',
      header: {
        'Authorization': token ? `Bearer ${token}` : '',
        'Cookie': cookieHeader
      },
      success(res) {
        const { statusCode, header: responseHeaders } = res;
        
        // 从响应头中保存 PHPSESSID
        savePhpSessIdFromHeaders(responseHeaders);
        
        if (statusCode === 200) {
          const data = JSON.parse(res.data);
          resolve(data);
        } else {
          const data = JSON.parse(res.data);
          wx.showToast({
            title: data.message || '上传失败',
            icon: 'none'
          });
          reject(new Error(data.message));
        }
      },
      fail(err) {
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
}

module.exports = {
  request,
  get,
  post,
  put,
  patch,
  del,
  uploadImage
};
