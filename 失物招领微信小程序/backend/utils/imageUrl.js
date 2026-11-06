// 图片 URL 工具函数
const config = require('../config');

/**
 * 格式化图片 URL
 * 如果是相对路径，转换为完整 URL
 * 如果已经是完整 URL，则原样返回
 * @param {string} imageUrl - 图片 URL
 * @param {object} req - Express 请求对象（可选，用于动态获取 host）
 * @returns {string} 格式化后的完整 URL
 */
function formatImageUrl(imageUrl, req = null) {
  if (!imageUrl) return imageUrl;
  
  // 如果已经是完整 URL（以 http:// 或 https:// 开头），直接返回
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }
  
  // 构建完整 URL
  let baseUrl = config.server.baseUrl || 'http://localhost:3000';
  
  // 如果提供了 req，从 req 中获取更准确的 host
  if (req) {
    const protocol = req.protocol || 'http';
    const host = req.get('host') || 'localhost:3000';
    baseUrl = `${protocol}://${host}`;
  }
  
  // 确保路径以 / 开头
  const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
  
  return `${baseUrl}${path}`;
}

/**
 * 批量格式化图片 URL 数组
 * @param {string[]} imageUrls - 图片 URL 数组
 * @param {object} req - Express 请求对象（可选）
 * @returns {string[]} 格式化后的 URL 数组
 */
function formatImageUrls(imageUrls, req = null) {
  if (!imageUrls || !Array.isArray(imageUrls)) {
    return [];
  }
  return imageUrls.map(url => formatImageUrl(url, req));
}

module.exports = {
  formatImageUrl,
  formatImageUrls
};
