// utils/util.js - 通用工具函数
const formatTime = (date) => {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = date.getHours();
  const minute = date.getMinutes();

  return `${[year, month, day].map(formatNumber).join('-')} ${[hour, minute].map(formatNumber).join(':')}`;
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${year}-${formatNumber(month)}-${formatNumber(day)}`;
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return formatTime(date);
};

const formatNumber = n => {
  return n >= 10 ? n : `0${n}`;
};

/**
 * 相对时间格式化
 */
const formatRelativeTime = (dateStr) => {
  if (!dateStr) return '';
  const now = new Date();
  const date = new Date(dateStr);
  const diff = now.getTime() - date.getTime();
  
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  
  if (seconds < 60) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 30) return `${days}天前`;
  return formatDate(dateStr);
};

/**
 * 手机号脱敏
 */
const maskPhone = (phone) => {
  if (!phone || phone.length < 7) return phone || '';
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4);
};

/**
 * 微信号脱敏
 */
const maskWechat = (wechat) => {
  if (!wechat || wechat.length < 4) return wechat || '';
  return wechat.substring(0, 3) + '***';
};

/**
 * 显示 Toast 提示
 */
const showToast = (title, icon = 'none') => {
  wx.showToast({
    title,
    icon,
    duration: 2000
  });
};

/**
 * 显示确认弹窗
 */
const showConfirm = (title, content = '确定执行此操作吗？') => {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      success(res) {
        resolve(res.confirm);
      }
    });
  });
};

/**
 * 物品类型文本
 */
const getItemTypeText = (type) => {
  return type === 'LOST' ? '寻物' : '拾物';
};

/**
 * 审核状态文本
 */
const getAuditStatusText = (status) => {
  const map = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  };
  return map[status] || status;
};

/**
 * 审核状态颜色
 */
const getAuditStatusColor = (status) => {
  const map = {
    'PENDING': '#faad14',
    'APPROVED': '#52c41a',
    'REJECTED': '#ff4d4f'
  };
  return map[status] || '#999';
};

/**
 * 业务状态文本（结合审核状态）
 * 规则：业务终止状态（CANCELLED/RESOLVED/RETURNED）优先于审核状态
 *       只有 bizStatus=ONGOING 时，才根据 auditStatus 显示审核相关状态
 */
const getBizStatusText = (status, itemType, auditStatus) => {
  // 1. 业务终止状态优先显示（取消/找回/归还不依赖审核状态）
  const terminationStatuses = ['CANCELLED', 'RESOLVED', 'RETURNED'];
  if (terminationStatuses.includes(status)) {
    const terminationMap = {
      'RESOLVED': '已找回',
      'RETURNED': '已归还',
      'CANCELLED': '已取消'
    };
    return terminationMap[status] || status;
  }

  // 2. ONGOING 状态下，审核状态决定显示
  if (auditStatus === 'PENDING') {
    return '待发布（未审核）';
  }
  if (auditStatus === 'REJECTED') {
    return '已驳回';
  }
  // ONGOING + APPROVED → 进行中
  return '进行中';
};

/**
 * 业务状态颜色（结合审核状态）
 * 规则：业务终止状态优先显示自身颜色
 *       只有 bizStatus=ONGOING 时，才根据 auditStatus 显示审核相关颜色
 */
const getBizStatusColor = (status, auditStatus) => {
  // 1. 业务终止状态使用自身颜色
  if (status === 'CANCELLED') {
    return '#999';
  }
  if (status === 'RESOLVED' || status === 'RETURNED') {
    return '#52c41a';
  }

  // 2. ONGOING 状态下，根据审核状态显示
  if (auditStatus === 'REJECTED') {
    return '#ff4d4f';
  }
  // ONGOING + PENDING/APPROVED → 蓝色（进行中）
  return '#1890FF';
};

/**
 * 通知类型文本
 */
const getNotificationTypeText = (type) => {
  const map = {
    'AUDIT_PASS': '审核通过',
    'AUDIT_REJECT': '审核驳回',
    'NEW_MESSAGE': '新消息',
    'REPORT_RESULT': '举报结果',
    'SYSTEM': '系统通知'
  };
  return map[type] || type;
};

module.exports = {
  formatTime,
  formatDate,
  formatDateTime,
  formatRelativeTime,
  formatNumber,
  maskPhone,
  maskWechat,
  showToast,
  showConfirm,
  getItemTypeText,
  getAuditStatusText,
  getAuditStatusColor,
  getBizStatusText,
  getBizStatusColor,
  getNotificationTypeText
};
