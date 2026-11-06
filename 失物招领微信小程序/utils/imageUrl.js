// 图片 URL 工具函数
const BASE_URL = 'http://localhost:3000';

/**
 * 格式化图片 URL
 * 如果是相对路径，转换为完整 URL
 * 如果已经是完整 URL，则原样返回
 * @param {string} imageUrl - 图片 URL
 * @returns {string} 格式化后的完整 URL
 */
function formatImageUrl(imageUrl) {
  if (!imageUrl) return imageUrl;
  
  // 如果已经是完整 URL（以 http:// 或 https:// 开头），直接返回
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }
  
  // 确保路径以 / 开头
  const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
  
  return `${BASE_URL}${path}`;
}

module.exports = {
  formatImageUrl
};
