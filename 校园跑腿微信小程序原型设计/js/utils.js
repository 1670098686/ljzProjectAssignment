// 校园跑腿小程序 - 工具函数库

// 清空模拟数据存储
const MockData = {
  // 用户数据
  users: {},

  // 任务数据
  tasks: []
      distance: 1.5
    }
  ],

  // 消息数据
  messages: [
    {
      id: 'msg_001',
      type: 'system',
      title: '系统通知',
      content: '欢迎使用校园跑腿小程序！',
      time: '2024-01-15 08:00:00',
      isRead: false
    },
    {
      id: 'msg_002',
      type: 'task',
      title: '任务状态更新',
      content: '您的任务"取快递到3号宿舍楼"已被接单',
      time: '2024-01-15 10:35:00',
      isRead: false
    }
  ]
};

// 本地存储工具
const Storage = {
  // 设置存储
  set: function(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch (e) {
      console.error('Storage set error:', e);
      return false;
    }
  },

  // 获取存储
  get: function(key, defaultValue = null) {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : defaultValue;
    } catch (e) {
      console.error('Storage get error:', e);
      return defaultValue;
    }
  },

  // 删除存储
  remove: function(key) {
    try {
      localStorage.removeItem(key);
      return true;
    } catch (e) {
      console.error('Storage remove error:', e);
      return false;
    }
  },

  // 清空存储
  clear: function() {
    try {
      localStorage.clear();
      return true;
    } catch (e) {
      console.error('Storage clear error:', e);
      return false;
    }
  }
};

// 弹窗管理工具
const Dialog = {
  // 获取图标
  getIcon: function(type) {
    const icons = {
      success: '<img src="images/icon-success.svg" class="icon-img">',
      error: '<img src="images/icon-error.svg" class="icon-img">',
      warning: '<img src="images/icon-warning.svg" class="icon-img">',
      info: '<img src="images/icon-info.svg" class="icon-img">'
    };
    return icons[type] || icons.info;
  },
  
  // 显示提示弹窗
  showToast: function(message, type = 'info', duration = 2000) {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <div class="toast-content">
        <span class="toast-icon">${this.getIcon(type)}</span>
        <span class="toast-message">${message}</span>
      </div>
    `;
    
    document.body.appendChild(toast);
    
    // 显示动画
    setTimeout(() => toast.classList.add('show'), 10);
    
    // 自动隐藏
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => {
        if (toast.parentNode) {
          toast.parentNode.removeChild(toast);
        }
      }, 300);
    }, duration);
  },

  // 显示确认弹窗
  showConfirm: function(options) {
    return new Promise((resolve) => {
      const modal = document.createElement('div');
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-content">
          <div class="modal-header">
            <h3>${options.title || '确认操作'}</h3>
          </div>
          <div class="modal-body">
            <p>${options.content || '请确认您的操作'}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary cancel-btn">${options.cancelText || '取消'}</button>
            <button class="btn btn-primary confirm-btn">${options.confirmText || '确认'}</button>
          </div>
        </div>
      `;
      
      document.body.appendChild(modal);
      
      // 显示动画
      setTimeout(() => modal.classList.add('show'), 10);
      
      // 绑定事件
      const confirmBtn = modal.querySelector('.confirm-btn');
      const cancelBtn = modal.querySelector('.cancel-btn');
      
      const closeModal = (result) => {
        modal.classList.remove('show');
        setTimeout(() => {
          if (modal.parentNode) {
            modal.parentNode.removeChild(modal);
          }
          resolve(result);
        }, 300);
      };
      
      confirmBtn.onclick = () => closeModal(true);
      cancelBtn.onclick = () => closeModal(false);
      
      // 点击遮罩层关闭
      modal.onclick = (e) => {
        if (e.target === modal) {
          closeModal(false);
        }
      };
    });
  },

  // 获取图标
  getIcon: function(type) {
    const icons = {
      info: '<img src="images/icon-info.svg" class="icon-img">',
      success: '<img src="images/icon-success.svg" class="icon-img">',
      warning: '<img src="images/icon-warning.svg" class="icon-img">',
      error: '<img src="images/icon-error.svg" class="icon-img">'
    };
    return icons[type] || icons.info;
  },

  // 显示自定义弹窗
  show: function(options) {
    return new Promise((resolve) => {
      const modal = document.createElement('div');
      modal.className = 'modal-overlay';
      
      // 构建按钮HTML
      const buttonsHTML = options.buttons ? options.buttons.map(btn => 
        `<button class="btn ${btn.type === 'primary' ? 'btn-primary' : 'btn-secondary'}" onclick="Dialog.close('${btn.text}')">${btn.text}</button>`
      ).join('') : '';
      
      modal.innerHTML = `
        <div class="modal-content">
          <div class="modal-header">
            <h3>${options.title || '提示'}</h3>
            <button class="modal-close" onclick="Dialog.close()">×</button>
          </div>
          <div class="modal-body">
            ${options.content || ''}
          </div>
          ${buttonsHTML ? `<div class="modal-footer">${buttonsHTML}</div>` : ''}
        </div>
      `;
      
      document.body.appendChild(modal);
      
      // 显示动画
      setTimeout(() => modal.classList.add('show'), 10);
      
      // 绑定关闭事件
      const closeModal = (result) => {
        modal.classList.remove('show');
        setTimeout(() => {
          if (modal.parentNode) {
            modal.parentNode.removeChild(modal);
          }
          resolve(result);
        }, 300);
      };
      
      // 存储关闭函数供外部调用
      this.currentModal = { close: closeModal };
      
      // 点击遮罩层关闭
      modal.onclick = (e) => {
        if (e.target === modal) {
          closeModal(false);
        }
      };
      
      return modal;
    });
  },

  // 关闭弹窗
  close: function(result = false) {
    if (this.currentModal && this.currentModal.close) {
      this.currentModal.close(result);
      this.currentModal = null;
    }
  }
};

// 表单验证工具
const Validator = {
  // 手机号验证
  validatePhone: function(phone) {
    const phoneRegex = /^1[3-9]\d{9}$/;
    return phoneRegex.test(phone);
  },

  // 邮箱验证
  validateEmail: function(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  },

  // 密码验证
  validatePassword: function(password) {
    return password.length >= 6;
  },

  // 验证码验证（模拟验证，固定验证码：123456）
  validateCode: function(code) {
    return code === '123456';
  },

  // 非空验证
  validateRequired: function(value) {
    return value && value.trim().length > 0;
  }
};

// 工具函数
const Utils = {
  // 格式化时间
  formatTime: function(date, format = 'YYYY-MM-DD HH:mm:ss') {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hour = String(d.getHours()).padStart(2, '0');
    const minute = String(d.getMinutes()).padStart(2, '0');
    const second = String(d.getSeconds()).padStart(2, '0');
    
    return format
      .replace('YYYY', year)
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hour)
      .replace('mm', minute)
      .replace('ss', second);
  },

  // 计算距离现在的时间
  formatRelativeTime: function(date) {
    const now = new Date();
    const target = new Date(date);
    const diff = now - target;
    
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 7) return `${days}天前`;
    
    return this.formatTime(date, 'MM-DD HH:mm');
  },
  
  // 增强版返回上一页函数，处理导航失败情况
  goBack: function() {
    try {
      // 保存当前URL用于检查导航是否成功
      const currentUrl = window.location.href;
      
      // 执行返回操作
      history.back();
      
      // 设置一个短暂的超时来检查导航是否成功
      setTimeout(() => {
        // 如果URL没有改变，说明返回失败
        if (window.location.href === currentUrl) {
          // 显示提示并返回首页
          if (typeof Dialog !== 'undefined') {
            Dialog.showConfirm({
              title: '导航提示',
              content: '无法返回上一页，将返回首页',
              confirmText: '确定',
              cancelText: '取消'
            }).then(confirm => {
              if (confirm) {
                window.location.href = '首页.html';
              }
            });
          } else {
            // 降级处理：使用alert
            if (confirm('无法返回上一页，是否返回首页？')) {
              window.location.href = '首页.html';
            }
          }
        }
      }, 100);
    } catch (error) {
      // 捕获任何导航错误
      if (typeof Dialog !== 'undefined') {
        Dialog.showConfirm({
          title: '导航错误',
          content: '导航失败，将返回首页',
          confirmText: '确定',
          cancelText: '取消'
        }).then(confirm => {
          if (confirm) {
            window.location.href = '首页.html';
          }
        });
      } else {
        // 降级处理：使用alert
        if (confirm('导航失败，是否返回首页？')) {
          window.location.href = '首页.html';
        }
      }
    }
  },

  // 生成唯一ID
  generateId: function(prefix = '') {
    return prefix + Date.now() + Math.random().toString(36).substr(2, 9);
  },

  // 防抖函数
  debounce: function(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  },

  // 节流函数
  throttle: function(func, limit) {
    let inThrottle;
    return function(...args) {
      if (!inThrottle) {
        func.apply(this, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  },

  // 深拷贝
  deepClone: function(obj) {
    if (obj === null || typeof obj !== 'object') return obj;
    if (obj instanceof Date) return new Date(obj.getTime());
    if (obj instanceof Array) return obj.map(item => this.deepClone(item));
    
    const cloned = {};
    for (let key in obj) {
      if (obj.hasOwnProperty(key)) {
        cloned[key] = this.deepClone(obj[key]);
      }
    }
    return cloned;
  }
};

// 页面路由管理
const Router = {
  // 跳转到页面
  navigateTo: function(url) {
    window.location.href = url;
  },

  // 返回上一页
  navigateBack: function() {
    window.history.back();
  },

  // 重定向到页面
  redirectTo: function(url) {
    window.location.replace(url);
  }
};

// 初始化应用
const App = {
  // 初始化应用
  init: function() {
    // 检查用户登录状态
    this.checkLoginStatus();
    
    // 初始化事件监听
    this.initEventListeners();
    
    // 显示加载完成提示
    Dialog.showToast('应用加载完成', 'success');
  },

  // 检查登录状态
  checkLoginStatus: function() {
    const userInfo = Storage.get('userInfo');
    const isLoggedIn = Storage.get('isLoggedIn', false);
    
    if (!isLoggedIn && !window.location.href.includes('登录.html')) {
      // 未登录且不在登录页，跳转到登录页
      Router.redirectTo('登录.html');
    }
  },

  // 初始化事件监听
  initEventListeners: function() {
    // 全局点击事件处理
    document.addEventListener('click', (e) => {
      // 处理返回按钮
      if (e.target.classList.contains('back-btn')) {
        Router.navigateBack();
      }
      
      // 处理链接跳转
      if (e.target.tagName === 'A' && e.target.href) {
        e.preventDefault();
        Router.navigateTo(e.target.href);
      }
    });
    
    // 表单提交事件
    document.addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleFormSubmit(e.target);
    });
  },

  // 处理表单提交
  handleFormSubmit: function(form) {
    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
      submitBtn.disabled = true;
    }
    
    // 模拟表单提交处理
    setTimeout(() => {
      Dialog.showToast('操作成功', 'success');
      if (submitBtn) {
        submitBtn.disabled = false;
      }
    }, 1000);
  }
};

// 全局状态管理
const StateManager = {
  // 当前用户信息
  currentUser: null,
  
  // 当前任务信息
  currentTask: null,
  
  // 页面状态
  pageState: {
    currentPage: '',
    prevPage: '',
    pageParams: {}
  },
  
  // 初始化状态
  init: function() {
    this.loadUserState();
    this.loadTaskState();
  },
  
  // 加载用户状态
  loadUserState: function() {
    const userInfo = Storage.get('userInfo');
    if (userInfo) {
      this.currentUser = userInfo;
    }
  },
  
  // 加载任务状态
  loadTaskState: function() {
    const taskInfo = Storage.get('currentTask');
    if (taskInfo) {
      this.currentTask = taskInfo;
    }
  },
  
  // 设置当前用户
  setCurrentUser: function(user) {
    this.currentUser = user;
    Storage.set('userInfo', user);
    Storage.set('isLoggedIn', true);
  },
  
  // 设置当前任务
  setCurrentTask: function(task) {
    this.currentTask = task;
    Storage.set('currentTask', task);
  },
  
  // 清除用户状态
  clearUserState: function() {
    this.currentUser = null;
    Storage.remove('userInfo');
    Storage.remove('isLoggedIn');
  },
  
  // 清除任务状态
  clearTaskState: function() {
    this.currentTask = null;
    Storage.remove('currentTask');
  }
};

// 事件总线 - 用于组件间通信
const EventBus = {
  events: {},
  
  // 监听事件
  on: function(event, callback) {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(callback);
  },
  
  // 触发事件
  emit: function(event, data) {
    if (this.events[event]) {
      this.events[event].forEach(callback => {
        callback(data);
      });
    }
  },
  
  // 移除事件监听
  off: function(event, callback) {
    if (this.events[event]) {
      this.events[event] = this.events[event].filter(cb => cb !== callback);
    }
  }
};

// 数据同步管理器
const DataSync = {
  // 同步用户数据
  syncUserData: function() {
    const users = Storage.get('users', MockData.users);
    const tasks = Storage.get('tasks', MockData.tasks);
    
    // 更新当前用户的任务统计
    if (StateManager.currentUser) {
      const userId = StateManager.currentUser.id;
      const publishedTasks = tasks.filter(task => task.publisherId === userId);
      const acceptedTasks = tasks.filter(task => task.runnerId === userId);
      const completedTasks = tasks.filter(task => 
        task.status === 'completed' && 
        (task.publisherId === userId || task.runnerId === userId)
      );
      
      // 更新用户统计数据
      StateManager.currentUser.tasksPublished = publishedTasks.length;
      StateManager.currentUser.tasksAccepted = acceptedTasks.length;
      StateManager.currentUser.tasksCompleted = completedTasks.length;
      
      // 计算信用分数
      StateManager.currentUser.creditScore = this.calculateCreditScore(StateManager.currentUser);
      
      Storage.set('userInfo', StateManager.currentUser);
    }
  },
  
  // 计算信用分数
  calculateCreditScore: function(user) {
    let score = 80; // 基础分数
    
    // 根据任务完成情况加分
    if (user.tasksCompleted > 0) {
      score += Math.min(20, user.tasksCompleted * 2);
    }
    
    // 根据平均评分加分
    if (user.averageRating) {
      score += Math.min(10, (user.averageRating - 4) * 5);
    }
    
    return Math.min(100, score);
  },
  
  // 同步任务数据
  syncTaskData: function() {
    const tasks = Storage.get('tasks', MockData.tasks);
    
    // 更新任务状态
    tasks.forEach(task => {
      if (task.status === 'in_progress') {
        // 模拟任务进度更新
        const progress = Math.random() * 100;
        task.progress = Math.min(100, progress);
        
        // 如果进度达到100%，完成任务
        if (task.progress >= 100) {
          task.status = 'completed';
          task.completeTime = new Date().toISOString();
          
          // 发送完成通知
          this.sendTaskCompleteNotification(task);
        }
      }
    });
    
    Storage.set('tasks', tasks);
  },
  
  // 发送任务完成通知
  sendTaskCompleteNotification: function(task) {
    const messages = Storage.get('messages', MockData.messages);
    
    const newMessage = {
      id: CommonUtils.generateId('msg_'),
      type: 'task',
      title: '任务已完成',
      content: `您的任务"${task.title}"已完成，请及时确认`, 
      time: new Date().toISOString(),
      isRead: false
    };
    
    messages.unshift(newMessage);
    Storage.set('messages', messages);
    
    // 触发消息更新事件
    EventBus.emit('messageUpdate', messages);
  },
  
  // 启动数据同步
  startSync: function() {
    // 每30秒同步一次数据
    setInterval(() => {
      this.syncUserData();
      this.syncTaskData();
    }, 30000);
  }
};

// 模拟API服务
const ApiService = {
  // 模拟登录
  login: function(openid, userInfo) {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 模拟登录成功
        const user = {
          id: CommonUtils.generateId('user_'),
          openid: openid,
          nickname: userInfo.nickName || '校园用户',
          avatar: userInfo.avatarUrl || '#',
          phone: '',
          role: 'publisher', // 默认身份
          creditScore: 80,
          tasksPublished: 0,
          tasksCompleted: 0
        };
        
        StateManager.setCurrentUser(user);
        resolve({ success: true, data: user });
      }, 1000);
    });
  },
  
  // 模拟绑定手机号
  bindPhone: function(phone, code) {
    return new Promise((resolve) => {
      setTimeout(() => {
        if (Validator.validatePhone(phone) && Validator.validateCode(code)) {
          if (StateManager.currentUser) {
            StateManager.currentUser.phone = phone;
            Storage.set('userInfo', StateManager.currentUser);
            resolve({ success: true });
          } else {
            resolve({ success: false, message: '用户未登录' });
          }
        } else {
          resolve({ success: false, message: '手机号或验证码错误' });
        }
      }, 800);
    });
  },
  
  // 模拟发布任务
  publishTask: function(taskData) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const tasks = Storage.get('tasks', MockData.tasks);
        
        const newTask = {
          id: CommonUtils.generateId('task_'),
          ...taskData,
          publisherId: StateManager.currentUser.id,
          status: 'pending',
          createTime: new Date().toISOString(),
          progress: 0
        };
        
        tasks.unshift(newTask);
        Storage.set('tasks', tasks);
        
        // 触发任务更新事件
        EventBus.emit('taskUpdate', tasks);
        
        resolve({ success: true, data: newTask });
      }, 500);
    });
  },
  
  // 模拟接单
  acceptTask: function(taskId) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const tasks = Storage.get('tasks', MockData.tasks);
        const taskIndex = tasks.findIndex(task => task.id === taskId);
        
        if (taskIndex !== -1) {
          tasks[taskIndex].runnerId = StateManager.currentUser.id;
          tasks[taskIndex].status = 'in_progress';
          tasks[taskIndex].acceptTime = new Date().toISOString();
          
          Storage.set('tasks', tasks);
          
          // 触发任务更新事件
          EventBus.emit('taskUpdate', tasks);
          
          resolve({ success: true, data: tasks[taskIndex] });
        } else {
          resolve({ success: false, message: '任务不存在' });
        }
      }, 500);
    });
  },
  
  // 模拟支付
  makePayment: function(taskId, paymentMethod) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const tasks = Storage.get('tasks', MockData.tasks);
        const taskIndex = tasks.findIndex(task => task.id === taskId);
        
        if (taskIndex !== -1) {
          tasks[taskIndex].paymentStatus = 'paid';
          tasks[taskIndex].paymentMethod = paymentMethod;
          tasks[taskIndex].paymentTime = new Date().toISOString();
          
          Storage.set('tasks', tasks);
          
          resolve({ success: true, data: tasks[taskIndex] });
        } else {
          resolve({ success: false, message: '任务不存在' });
        }
      }, 1000);
    });
  }
};

// 页面初始化函数
function initPage() {
  // 初始化状态管理器
  StateManager.init();
  
  // 启动数据同步
  DataSync.startSync();
  
  // 初始化应用
  App.init();
}

// 页面加载完成后初始化应用
document.addEventListener('DOMContentLoaded', function() {
  App.init();
});