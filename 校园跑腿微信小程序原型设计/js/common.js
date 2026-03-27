// 通用工具函数和状态管理 - v1.2

// 模拟数据存储
class DataStore {
    constructor() {
        this.storage = window.localStorage;
        this.init();
        // 敏感信息加密密钥（实际项目中应使用更安全的密钥管理方式）
        this.encryptionKey = 'campus_runner_secret_key';
        // 标准化历史任务数据，避免字段缺失导致统计/过滤异常
        this.normalizeTasks();
    }

    init() {
        // 仅在首次缺省时初始化，避免每次加载都清空历史数据
        if (this.storage.getItem('tasks') === null) {
            this.set('tasks', []);
        }
        if (this.storage.getItem('users') === null) {
            this.set('users', []);
        }
        if (this.storage.getItem('currentUser') === null) {
            this.set('currentUser', null);
        }
        if (this.storage.getItem('tasksCleared') === null) {
            // 默认不清空任务，保持已有数据
            this.storage.setItem('tasksCleared', 'false');
        }
    }

    // 确保任务包含标准字段，兼容旧存量数据
    normalizeTasks() {
        try {
            const tasks = this.get('tasks');
            if (!Array.isArray(tasks)) return;
            const normalized = tasks.map((t, idx) => {
                const task = { ...t };
                if (!task.id) task.id = `task_${idx + 1}`;
                // 统一发布者字段
                const pub = task.publisherId || task.publisher || task.publisherName;
                task.publisherId = task.publisherId || pub || 'mock_publisher_zhangsan';
                task.publisherName = task.publisherName || task.publisher || pub || '张三';
                task.publisher = task.publisher || task.publisherName;
                // 统一接单人字段
                const runnerVal = task.runnerId || task.runner || task.runnerName;
                if (runnerVal) {
                    task.runnerId = task.runnerId || runnerVal;
                    task.runnerName = task.runnerName || task.runner || runnerVal;
                    task.runner = task.runner || task.runnerName;
                }
                // 默认状态
                task.status = task.status || 'pending';
                return task;
            });
            this.set('tasks', normalized);
        } catch (e) {
            console.warn('normalizeTasks failed', e);
        }
    }

    get(key) {
        const item = this.storage.getItem(key);
        if (!item) return null;
        
        try {
            // 如果是敏感信息，进行解密
            if (['currentUser', 'users'].includes(key)) {
                const encryptedData = JSON.parse(item);
                if (encryptedData.encrypted) {
                    const decryptedData = CommonUtils.decrypt(encryptedData.data);
                    return JSON.parse(decryptedData);
                }
            }
            return JSON.parse(item);
        } catch (error) {
            console.error('获取数据失败:', error);
            return null;
        }
    }

    set(key, value) {
        try {
            const dataString = JSON.stringify(value);
            
            // 如果是敏感信息，进行加密
            if (['currentUser', 'users'].includes(key)) {
                const encryptedData = {
                    encrypted: true,
                    data: CommonUtils.encrypt(dataString),
                    timestamp: Date.now()
                };
                this.storage.setItem(key, JSON.stringify(encryptedData));
            } else {
                this.storage.setItem(key, dataString);
            }
        } catch (error) {
            console.error('存储数据失败:', error);
        }
    }

    // 清除当前任务并按需重建
    resetTasks({ reseed = true, auth = null } = {}) {
        if (reseed) {
            const newTasks = this.buildTasksForAuth(auth || CommonUtils.getAuth() || {});
            this.set('tasks', newTasks);
        } else {
            this.set('tasks', []);
        }
        return this.get('tasks') || [];
    }

    remove(key) {
        this.storage.removeItem(key);
    }

    // 针对当前账号清理任务相关缓存，避免不同账号数据串扰
    clearTaskCaches() {
        ['tasks', 'currentTask', 'currentTaskId', 'taskMessages', 'tasksCleared', 'lastPublishedTaskId', 'lastPublishedPrice', 'lastPublishedTitle', 'messageThreads', 'evaluations', 'evaluationStats', 'runners', 'appState', 'currentUser']
            .forEach(k => this.remove(k));
    }

    // 获取分页任务数据
    getTasks(page = 1, pageSize = 10, filters = {}) {
        const allTasks = this.get('tasks') || [];
        
        // 应用过滤条件
        let filteredTasks = allTasks.filter(task => {
            // 状态过滤
            if (filters.status && task.status !== filters.status) {
                return false;
            }
            // 类型过滤
            if (filters.type && task.type !== filters.type) {
                return false;
            }
            // 紧急状态过滤
            if (filters.urgent !== undefined && task.urgent !== filters.urgent) {
                return false;
            }
            // 价格范围过滤
            if (filters.minPrice && task.price < filters.minPrice) {
                return false;
            }
            if (filters.maxPrice && task.price > filters.maxPrice) {
                return false;
            }
            return true;
        });
        
        // 排序
        filteredTasks.sort((a, b) => {
            // 紧急任务优先，然后按创建时间倒序
            if (a.urgent && !b.urgent) return -1;
            if (!a.urgent && b.urgent) return 1;
            return b.createTime - a.createTime;
        });
        
        // 分页
        const startIndex = (page - 1) * pageSize;
        const endIndex = startIndex + pageSize;
        const paginatedTasks = filteredTasks.slice(startIndex, endIndex);
        
        return {
            tasks: paginatedTasks,
            total: filteredTasks.length,
            page: page,
            pageSize: pageSize,
            totalPages: Math.ceil(filteredTasks.length / pageSize)
        };
    }

    // 清空模拟任务数据
    getMockTasks() {
        return [];
    }

    // 创建有效的未接单任务
    buildTasksForAuth(auth = {}) {
        const tasks = [
            {
                id: 'task_001',
                type: '快递',
                title: '取快递到图书馆',
                from: '东门快递点',
                to: '图书馆一楼',
                price: 8.00,
                distance: 350,
                status: 'pending',
                publisherId: 'mock_publisher_wangwu',
                publisherName: '王五',
                createTime: new Date(Date.now() - 30 * 60 * 1000).toISOString(), // 30分钟前
                deadline: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(), // 2小时后
                description: '取一个中通快递包裹，送到图书馆一楼服务台',
                isUrgent: false
            },
            {
                id: 'task_002',
                type: '餐食',
                title: '食堂取餐到宿舍',
                from: '第一食堂二楼',
                to: '男生宿舍3号楼',
                price: 12.00,
                distance: 600,
                status: 'pending',
                publisherId: 'mock_publisher_lisi',
                publisherName: '李四',
                createTime: new Date(Date.now() - 15 * 60 * 1000).toISOString(), // 15分钟前
                deadline: new Date(Date.now() + 1 * 60 * 60 * 1000).toISOString(), // 1小时后
                description: '取一份麻辣香锅，送到宿舍楼下',
                isUrgent: true
            },
            {
                id: 'task_003',
                type: '文件',
                title: '送文件到行政楼',
                from: '教学楼A座',
                to: '行政楼三楼',
                price: 6.00,
                distance: 450,
                status: 'pending',
                publisherId: 'mock_publisher_teacher_zhang',
                publisherName: '张老师',
                createTime: new Date(Date.now() - 45 * 60 * 1000).toISOString(), // 45分钟前
                deadline: new Date(Date.now() + 1.5 * 60 * 60 * 1000).toISOString(), // 1.5小时后
                description: '送一份教学材料到教务处',
                isUrgent: false
            },
            {
                id: 'task_004',
                type: '其他',
                title: '买文具到教室',
                from: '校园超市',
                to: '教学楼C座301',
                price: 10.00,
                distance: 300,
                status: 'pending',
                publisherId: 'mock_publisher_zhaoliu',
                publisherName: '赵六',
                createTime: new Date(Date.now() - 10 * 60 * 1000).toISOString(), // 10分钟前
                deadline: new Date(Date.now() + 1 * 60 * 60 * 1000).toISOString(), // 1小时后
                description: '买两支黑色签字笔和一本笔记本',
                isUrgent: false
            },
            {
                id: 'task_005',
                type: '快递',
                title: '快递送到实验室',
                from: '西门快递柜',
                to: '实验楼B座205',
                price: 9.00,
                distance: 550,
                status: 'pending',
                publisherId: 'mock_publisher_chenqi',
                publisherName: '陈七',
                createTime: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5分钟前
                deadline: new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString(), // 3小时后
                description: '取一个顺丰快递，送到实验室',
                isUrgent: false
            },
            {
                id: 'task_006',
                type: '餐食',
                title: '奶茶送到图书馆',
                from: '校园奶茶店',
                to: '图书馆四楼自习区',
                price: 15.00,
                distance: 400,
                status: 'pending',
                publisherId: 'mock_publisher_sunba',
                publisherName: '孙八',
                createTime: new Date(Date.now() - 20 * 60 * 1000).toISOString(), // 20分钟前
                deadline: new Date(Date.now() + 40 * 60 * 1000).toISOString(), // 40分钟后
                description: '两杯珍珠奶茶，一杯少糖',
                isUrgent: true
            }
        ];
        
        return tasks;
    }

    // 清空固定种子任务
    getPersistentTasks() {
        return [];
    }

    // 清除任务与相关记录，并标记不再自动注入种子
    clearTasksAndRecords() {
        this.storage.setItem('tasksCleared', 'true');
        this.set('tasks', []);
        this.remove('currentTask');
        this.remove('currentTaskId');
        this.remove('taskMessages');
        this.remove('appState');
    }
}

// 弹窗管理器
class ModalManager {
    constructor() {
        this.modals = new Map();
    }

    // 显示弹窗
    show(modalId, options = {}) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        modal.classList.add('show');
        document.body.style.overflow = 'hidden';

        // 自动关闭设置
        if (options.autoClose) {
            setTimeout(() => {
                this.hide(modalId);
            }, options.duration || 3000);
        }

        this.modals.set(modalId, modal);
    }

    // 隐藏弹窗
    hide(modalId) {
        const modal = this.modals.get(modalId);
        if (modal) {
            modal.classList.remove('show');
            document.body.style.overflow = '';
            this.modals.delete(modalId);
        }
    }

    // 显示确认弹窗
    showConfirm(title, content, confirmText = '确认', cancelText = '取消') {
        return new Promise((resolve) => {
            const modal = document.createElement('div');
            modal.className = 'modal';
            modal.innerHTML = `
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 class="modal-title">${title}</h3>
                        <button class="modal-close" onclick="this.closest('.modal').remove()">×</button>
                    </div>
                    <div class="modal-body">
                        <p>${content}</p>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="this.closest('.modal').remove(); resolve(false)">${cancelText}</button>
                        <button class="btn btn-primary" onclick="this.closest('.modal').remove(); resolve(true)">${confirmText}</button>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
            setTimeout(() => modal.classList.add('show'), 10);
        });
    }

    // 显示提示弹窗
    showAlert(title, content, type = 'info') {
        const modal = document.createElement('div');
        modal.className = 'modal';
        
        let icon = '<img src="images/icon-info.svg" class="icon-img">';
        if (type === 'success') icon = '<img src="images/icon-success.svg" class="icon-img">';
        if (type === 'warning') icon = '<img src="images/icon-warning.svg" class="icon-img">';
        if (type === 'error') icon = '<img src="images/icon-error.svg" class="icon-img">';

        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3 class="modal-title">${icon} ${title}</h3>
                    <button class="modal-close" onclick="this.closest('.modal').remove()">×</button>
                </div>
                <div class="modal-body">
                    <p>${content}</p>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary btn-block" onclick="this.closest('.modal').remove()">知道了</button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        setTimeout(() => modal.classList.add('show'), 10);
    }
}

// 表单验证工具
class FormValidator {
    static validatePhone(phone) {
        const phoneRegex = /^1[3-9]\d{9}$/;
        if (!phone) {
            return { valid: false, message: '请输入手机号' };
        } else if (!phoneRegex.test(phone)) {
            return { valid: false, message: '请输入正确的11位手机号' };
        }
        return { valid: true, message: '' };
    }

    static validatePassword(password) {
        if (!password) {
            return { valid: false, message: '请输入密码' };
        } else if (password.length < 6) {
            return { valid: false, message: '密码长度不能少于6位' };
        } else if (password.length > 20) {
            return { valid: false, message: '密码长度不能超过20位' };
        }
        return { valid: true, message: '' };
    }

    static validateEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email) {
            return { valid: false, message: '请输入邮箱' };
        } else if (!emailRegex.test(email)) {
            return { valid: false, message: '请输入正确的邮箱格式' };
        }
        return { valid: true, message: '' };
    }

    static validateRequired(value, fieldName = '此项') {
        if (!value || value.trim().length === 0) {
            return { valid: false, message: `${fieldName}不能为空` };
        }
        return { valid: true, message: '' };
    }

    static validateNumber(value, min = 0, max = Infinity, fieldName = '数值') {
        const num = Number(value);
        if (!value) {
            return { valid: false, message: `${fieldName}不能为空` };
        } else if (isNaN(num)) {
            return { valid: false, message: `${fieldName}必须是数字` };
        } else if (num < min) {
            return { valid: false, message: `${fieldName}不能小于${min}` };
        } else if (num > max) {
            return { valid: false, message: `${fieldName}不能大于${max}` };
        }
        return { valid: true, message: '' };
    }

    static validateVerificationCode(code, length = 6) {
        if (!code) {
            return { valid: false, message: '请输入验证码' };
        } else if (code.length !== length) {
            return { valid: false, message: `请输入${length}位验证码` };
        } else if (!/^\d+$/.test(code)) {
            return { valid: false, message: '验证码必须是数字' };
        }
        return { valid: true, message: '' };
    }
}

// 通用工具函数
class CommonUtils {
    // 格式化时间
    static formatTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;

        if (diff < 60000) { // 1分钟内
            return '刚刚';
        } else if (diff < 3600000) { // 1小时内
            return Math.floor(diff / 60000) + '分钟前';
        } else if (diff < 86400000) { // 1天内
            return Math.floor(diff / 3600000) + '小时前';
        } else if (diff < 2592000000) { // 30天内
            return Math.floor(diff / 86400000) + '天前';
        } else {
            return date.toLocaleDateString();
        }
    }

    // 格式化价格
    static formatPrice(price) {
        return '¥' + price.toFixed(2);
    }

    // 防抖函数
    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // 节流函数
    static throttle(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }

    // 生成随机ID
    static generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2);
    }

    // 深拷贝
    static deepClone(obj) {
        return JSON.parse(JSON.stringify(obj));
    }

    // 显示Toast提示
    static showToast(message, type = 'info', duration = 3000) {
        // 移除现有的toast
        const existingToast = document.querySelector('.toast');
        if (existingToast) {
            existingToast.remove();
        }
        
        // 创建新的toast
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        
        // 添加样式
        toast.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0, 0, 0, 0.8);
            color: white;
            padding: 12px 20px;
            border-radius: 8px;
            font-size: 14px;
            z-index: 9999;
            animation: toastSlideIn 0.3s ease;
        `;
        
        // 添加类型样式
        if (type === 'success') {
            toast.style.background = '#52c41a';
        } else if (type === 'error') {
            toast.style.background = '#ff4d4f';
        } else if (type === 'warning') {
            toast.style.background = '#faad14';
        }
        
        // 添加动画样式
        const style = document.createElement('style');
        style.textContent = `
            @keyframes toastSlideIn {
                from { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
                to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
            }
        `;
        document.head.appendChild(style);
        
        document.body.appendChild(toast);
        
        // 自动移除
        setTimeout(() => {
            toast.style.animation = 'toastSlideOut 0.3s ease forwards';
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, duration);
    }

    // 简单的加密函数（用于敏感信息存储）
    static encrypt(str) {
        // 使用简单的加密算法（实际项目中应使用更安全的加密方式）
        let encrypted = '';
        for (let i = 0; i < str.length; i++) {
            encrypted += String.fromCharCode(str.charCodeAt(i) + 1);
        }
        return encrypted;
    }

    // 解密函数
    static decrypt(str) {
        let decrypted = '';
        for (let i = 0; i < str.length; i++) {
            decrypted += String.fromCharCode(str.charCodeAt(i) - 1);
        }
        return decrypted;
    }

    /* 身份与路由守卫 */
    static getAuth() {
        const raw = localStorage.getItem('authUser');
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (e) {
            console.error('解析身份信息失败', e);
            return null;
        }
    }

    static setAuth(auth) {
        localStorage.setItem('authUser', JSON.stringify(auth));
    }

    // 依据当前账号ID为key加前缀，便于多账号隔离（仅用于新存储场景）
    static scopedKey(key, auth = null) {
        const a = auth || this.getAuth();
        const uid = a && (a.id || a.openid || a.phone || a.name);
        return uid ? `${uid}::${key}` : key;
    }

    static requireLogin(redirect = '登录.html') {
        const auth = this.getAuth();
        if (!auth || !auth.loggedIn) {
            alert('请先登录');
            window.location.href = redirect;
            return false;
        }
        return true;
    }

    static requireIdentityChosen(redirect = '身份选择.html') {
        const auth = this.getAuth();
        // 未登录则不强制
        if (!auth || !auth.loggedIn) return true;
        if (auth.firstLogin) {
            alert('请选择身份后再继续');
            window.location.href = redirect;
            return false;
        }
        return true;
    }

    static requireRunnerAccess(options = {}) {
        const { redirectIfNoRunner = '首页.html', redirectIfPending = '认证状态.html' } = options;
        const auth = this.getAuth();
        if (!auth || !auth.loggedIn) {
            alert('请先登录');
            window.location.href = '登录.html';
            return false;
        }
        // 仅跑腿员或双身份可访问
        if (!['runner', 'dual'].includes(auth.role)) {
            alert('该功能仅限跑腿员使用');
            window.location.href = redirectIfNoRunner;
            return false;
        }
        // 跑腿员必须审核通过
        if (auth.runnerStatus !== 'approved') {
            alert('跑腿员认证通过后才可使用此功能');
            window.location.href = redirectIfPending;
            return false;
        }
        return true;
    }

    // 管理员权限检查
    static requireAdminAccess(redirect = '首页.html') {
        const auth = this.getAuth();
        if (!auth || !auth.loggedIn) {
            alert('请先登录');
            window.location.href = '登录.html';
            return false;
        }
        // 仅管理员可访问
        if (auth.role !== 'admin') {
            alert('该功能仅限管理员使用');
            window.location.href = redirect;
            return false;
        }
        return true;
    }

    // 为需要跑腿员身份的元素添加统一守卫（需设置 data-runner-required 和可选 data-href）
    static bindRunnerGuards() {
        document.querySelectorAll('[data-runner-required]').forEach(el => {
            el.addEventListener('click', (e) => {
                const ok = CommonUtils.requireRunnerAccess();
                if (!ok) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }
                const targetHref = el.getAttribute('data-href');
                if (targetHref) {
                    e.preventDefault();
                    window.location.href = targetHref;
                }
                return true;
            });
        });
    }
}

// 通用图片上传器
class ImageUploader {
    constructor(options = {}) {
        this.container = typeof options.container === 'string' ? document.querySelector(options.container) : options.container;
        if (!this.container) return;

        this.maxCount = options.maxCount || 3;
        this.maxSizeMB = options.maxSizeMB || 5;
        this.previewSize = options.previewSize || 80;
        this.onChange = typeof options.onChange === 'function' ? options.onChange : () => {};
        this.files = [];

        this.init();
    }

    init() {
        // 基础布局
        if (!this.container.style.display) {
            this.container.style.display = 'flex';
        }
        if (!this.container.style.gap) {
            this.container.style.gap = '8px';
        }
        if (!this.container.style.flexWrap) {
            this.container.style.flexWrap = 'wrap';
        }

        // 触发按钮（优先使用 upload-add / placeholder-img）
        this.addButton = this.container.querySelector('.upload-add') || this.container.querySelector('.placeholder-img');
        if (this.addButton) {
            this.addButton.style.display = 'flex';
            this.addButton.style.alignItems = 'center';
            this.addButton.style.justifyContent = 'center';
            this.addButton.style.cursor = 'pointer';
        }

        // 隐藏文件选择器
        this.fileInput = document.createElement('input');
        this.fileInput.type = 'file';
        this.fileInput.accept = 'image/*';
        this.fileInput.multiple = this.maxCount > 1;
        this.fileInput.style.display = 'none';
        this.container.appendChild(this.fileInput);

        // 事件绑定
        const triggerTarget = this.addButton || this.container;
        triggerTarget.addEventListener('click', () => this.fileInput.click());
        this.fileInput.addEventListener('change', (e) => this.handleFiles(e.target.files));
    }

    handleFiles(fileList) {
        const incoming = Array.from(fileList || []);
        const remaining = this.maxCount - this.files.length;
        if (remaining <= 0) {
            CommonUtils.showToast(`最多上传${this.maxCount}张图片`, 'warning');
            this.fileInput.value = '';
            return;
        }

        incoming.slice(0, remaining).forEach(file => {
            if (file.size > this.maxSizeMB * 1024 * 1024) {
                CommonUtils.showToast(`单张图片大小不能超过${this.maxSizeMB}MB`, 'warning');
                return;
            }
            const reader = new FileReader();
            reader.onload = (ev) => {
                this.files.push({ file, preview: ev.target.result });
                this.render();
            };
            reader.readAsDataURL(file);
        });

        this.fileInput.value = '';
    }

    render() {
        // 清理旧预览
        this.container.querySelectorAll('.upload-preview').forEach(el => el.remove());

        const sizeValue = typeof this.previewSize === 'number' ? `${this.previewSize}px` : this.previewSize;
        const addBtn = this.addButton;

        this.files.forEach((item, index) => {
            const preview = document.createElement('div');
            preview.className = 'upload-item upload-preview';
            preview.style.cssText = `position:relative;width:${sizeValue};height:${sizeValue};border-radius:8px;overflow:hidden;background:#f5f7fa;`;
            preview.innerHTML = `
                <img src="${item.preview}" alt="已选图片" style="width:100%;height:100%;object-fit:cover;">
                <div class="upload-remove" style="position:absolute;top:-8px;right:-8px;width:20px;height:20px;background:#ff4d4f;color:#fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:12px;cursor:pointer;">×</div>
            `;
            preview.querySelector('.upload-remove').addEventListener('click', () => {
                this.files.splice(index, 1);
                this.render();
            });

            if (addBtn) {
                this.container.insertBefore(preview, addBtn);
            } else {
                this.container.appendChild(preview);
            }
        });

        if (this.addButton) {
            this.addButton.style.display = this.files.length >= this.maxCount ? 'none' : 'flex';
        }

        this.onChange(this.files);
    }

    getFiles() {
        return this.files.map(item => item.file);
    }

    getPreviews() {
        return this.files.map(item => item.preview);
    }

    reset() {
        this.files = [];
        this.render();
    }
}

// 统一的消息存储（系统通知/交易通知/私信）
const MessageStore = {
    storageKey: 'messageThreads',
    memoryThreads: null,

    getDefaults() {
        const now = new Date().toISOString();
        return {
            system: {
                id: 'system',
                title: '系统通知',
                unread: 0,
                lastText: '欢迎使用校园跑腿小程序！',
                lastTime: now,
                disableInput: true,
                messages: [
                    { id: CommonUtils.generateId(), side: 'other', text: '欢迎使用校园跑腿小程序！', time: now }
                ]
            },
            trade: {
                id: 'trade',
                title: '交易通知',
                unread: 0,
                lastText: '您有一笔新的收入到账，请查收。',
                lastTime: now,
                disableInput: true,
                messages: [
                    { id: CommonUtils.generateId(), side: 'other', text: '您有一笔新的收入到账，请查收。', time: now }
                ]
            },
            chat: {
                id: 'chat',
                title: '李同学',
                unread: 0,
                lastText: '你好，请问大概什么时候能送到？',
                lastTime: now,
                disableInput: false,
                messages: [
                    { id: CommonUtils.generateId(), side: 'other', text: '你好，请问大概什么时候能送到？', time: now },
                    { id: CommonUtils.generateId(), side: 'self', text: '你好，我已经取到件了，大概10分钟后到宿舍楼下。', time: now }
                ]
            }
        };
    },

    load() {
        try {
            const saved = localStorage.getItem(this.storageKey);
            if (!saved) {
                const defaults = this.getDefaults();
                this.save(defaults);
                return defaults;
            }
            const parsed = JSON.parse(saved);
            // 兜底保证结构完整，避免损坏导致渲染/发送失败
            const withDefaults = { ...this.getDefaults(), ...(parsed || {}) };
            Object.keys(withDefaults).forEach(key => {
                const t = withDefaults[key] || {};
                if (!Array.isArray(t.messages)) t.messages = [];
                if (typeof t.unread !== 'number') t.unread = 0;
                if (!t.id) t.id = key;
            });
            return withDefaults;
        } catch (e) {
            console.warn('消息数据损坏或存储不可用，切换为内存存储', e);
            if (!this.memoryThreads) {
                this.memoryThreads = this.getDefaults();
            }
            return this.memoryThreads;
        }
    },

    save(threads) {
        try {
            localStorage.setItem(this.storageKey, JSON.stringify(threads));
        } catch (e) {
            this.memoryThreads = threads;
        }
        if (typeof EventBus !== 'undefined') {
            EventBus.emit('messageUpdate', threads);
        }
        return threads;
    },

    ensure() {
        return this.load();
    },

    getThread(id) {
        const threads = this.load();
        return threads[id];
    },

    upsertThread(thread) {
        const threads = this.load();
        threads[thread.id] = {
            messages: [],
            unread: 0,
            lastText: '',
            lastTime: new Date().toISOString(),
            disableInput: false,
            ...thread
        };
        return this.save(threads)[thread.id];
    },

    markRead(id) {
        const threads = this.load();
        if (threads[id]) {
            threads[id].unread = 0;
            this.save(threads);
        }
    },

    markAllRead() {
        const threads = this.load();
        Object.keys(threads).forEach(key => {
            threads[key].unread = 0;
        });
        this.save(threads);
    },

    addMessage(threadId, text, side = 'self', extra = {}) {
        const threads = this.load();
        const now = new Date().toISOString();
        const thread = threads[threadId] || this.upsertThread({ id: threadId, title: extra.title || threadId });
        if (!Array.isArray(thread.messages)) thread.messages = [];
        const message = { id: CommonUtils.generateId(), text, side, time: now };
        if (extra.type) message.type = extra.type;
        if (extra.imageUrl) message.imageUrl = extra.imageUrl;
        thread.messages.push(message);
        thread.lastText = text;
        thread.lastTime = now;
        if (side === 'other') {
            thread.unread = (thread.unread || 0) + 1;
        }
        threads[threadId] = thread;
        this.save(threads);
        return message;
    }
};
// 暴露到全局，便于各页面直接访问
window.MessageStore = MessageStore;

// 状态管理器
class StateManager {
    constructor() {
        this.state = {
            currentUser: null,
            tasks: [],
            messages: [],
            currentPage: 'index',
            // 分页相关状态
            currentTasksPage: 1,
            tasksPerPage: 10,
            tasksFilters: {},
            totalTasks: 0,
            totalTasksPages: 1
        };
        this.listeners = [];
        this.dataStore = new DataStore();
        this.init();
    }

    init() {
        // 从本地存储加载状态
        const savedState = this.dataStore.get('appState');
        if (savedState) {
            this.state = { ...this.state, ...savedState };
        }

        // 加载当前用户
        this.state.currentUser = this.dataStore.get('currentUser');
        
        // 加载任务数据
        this.loadTasks();
    }

    setState(newState) {
        this.state = { ...this.state, ...newState };
        this.dataStore.set('appState', this.state);
        this.notifyListeners();
    }

    subscribe(listener) {
        this.listeners.push(listener);
        return () => {
            this.listeners = this.listeners.filter(l => l !== listener);
        };
    }

    notifyListeners() {
        this.listeners.forEach(listener => listener(this.state));
    }

    // 加载任务数据（支持分页）
    loadTasks(page = 1, filters = {}) {
        // 更新当前分页和过滤条件
        this.setState({
            currentTasksPage: page,
            tasksFilters: filters
        });
        
        // 使用分页获取任务
        const paginatedTasks = this.dataStore.getTasks(page, this.state.tasksPerPage, filters);
        this.setState({
            tasks: paginatedTasks.tasks,
            totalTasks: paginatedTasks.total,
            totalTasksPages: paginatedTasks.totalPages
        });
    }

    // 用户相关方法
    login(user) {
        this.setState({ currentUser: user });
        this.dataStore.set('currentUser', user);
        CommonUtils.showToast('登录成功', 'success');
    }

    logout() {
        this.setState({ currentUser: null });
        this.dataStore.remove('currentUser');
        CommonUtils.showToast('已退出登录', 'success');
    }

    // 任务相关方法
    addTask(task) {
        const newTask = {
            ...task,
            id: CommonUtils.generateId(),
            createTime: new Date().getTime(),
            status: 'pending'
        };

        // 始终基于完整任务列表追加，避免丢失分页之外的任务
        const existing = this.dataStore.get('tasks') || [];
        const tasks = [...existing, newTask];

        this.setState({ tasks });
        this.dataStore.set('tasks', tasks);
        // 新发布任务后允许再次显示任务列表，不再强制空状态
        this.dataStore.storage.setItem('tasksCleared', 'false');

        CommonUtils.showToast('任务发布成功', 'success');
        return newTask;
    }

    updateTask(taskId, updates) {
        const tasks = this.state.tasks.map(task =>
            task.id === taskId ? { ...task, ...updates } : task
        );
        this.setState({ tasks });
        this.dataStore.set('tasks', tasks);
        CommonUtils.showToast('任务更新成功', 'success');
    }

    acceptTask(taskId, runner) {
        this.updateTask(taskId, {
            status: 'in-progress',
            runner: runner.name,
            acceptTime: new Date().getTime()
        });
        CommonUtils.showToast('接单成功', 'success');
    }

    completeTask(taskId) {
        this.updateTask(taskId, {
            status: 'completed',
            completeTime: new Date().getTime()
        });
        CommonUtils.showToast('任务已完成', 'success');
    }

    // 获取任务详情
    getTaskById(taskId) {
        return this.state.tasks.find(task => task.id === taskId);
    }

    // 加载更多任务（分页加载）
    loadMoreTasks() {
        if (this.state.currentTasksPage < this.state.totalTasksPages) {
            const nextPage = this.state.currentTasksPage + 1;
            this.loadTasks(nextPage, this.state.tasksFilters);
        } else {
            CommonUtils.showToast('已经没有更多任务了', 'info');
        }
    }
}

// 全局实例
const dataStore = new DataStore();
const modalManager = new ModalManager();
const stateManager = new StateManager();

// 统一的任务流转工具，确保接单、送达、确认等逻辑在各页保持一致
const TaskFlow = {
    getSimulatedRunnerProfile() {
        const key = 'simRunnerProfile';
        try {
            const saved = JSON.parse(localStorage.getItem(key) || '{}');
            if (saved && saved.id && saved.name) return saved;
        } catch (e) {
            console.warn('读取模拟跑腿员信息失败，将使用默认值', e);
        }

        const fallback = {
            id: 'sim_runner_demo',
            name: '模拟跑腿员',
            phone: '13800000000'
        };

        try {
            localStorage.setItem(key, JSON.stringify(fallback));
        } catch (e) {
            console.warn('保存模拟跑腿员信息失败', e);
        }

        return fallback;
    },

    normalizeId(value) {
        if (value === null || value === undefined) return '';
        return String(value).trim();
    },

    normalizeTaskIds(task) {
        const normalized = { ...task };
        const publisherId = task.publisherId || task.publisher || task.publisherName;
        const runnerId = task.runnerId || task.runner || task.runnerName;

        if (publisherId && !task.publisherId) {
            normalized.publisherId = publisherId;
        }
        if (!task.publisherName && (task.publisher || task.publisherId)) {
            normalized.publisherName = task.publisher || task.publisherId;
        }
        if (runnerId && !task.runnerId) {
            normalized.runnerId = runnerId;
        }
        if (!task.runnerName && (task.runner || task.runnerId)) {
            normalized.runnerName = task.runner || task.runnerId;
        }
        return normalized;
    },

    ensureSeedTasks(auth = CommonUtils.getAuth()) {
        const existing = dataStore.get('tasks');
        const cleared = dataStore.storage.getItem('tasksCleared') === 'true';
        if (Array.isArray(existing) && existing.length > 0 && !cleared) {
            return existing;
        }

        const seeds = dataStore.buildTasksForAuth(auth || {});
        dataStore.set('tasks', seeds);
        dataStore.storage.setItem('tasksCleared', 'false');
        return seeds;
    },

    getTasks() {
        return dataStore.get('tasks') || [];
    },

    saveTasks(tasks) {
        dataStore.set('tasks', tasks);
        if (window.stateManager) {
            stateManager.setState({ tasks });
        }
        return tasks;
    },

    findTask(taskId) {
        const tasks = this.getTasks();
        return tasks.find(t => String(t.id) === String(taskId) || String(t.taskId || '') === String(taskId));
    },

    updateTask(taskId, updater) {
        const tasks = this.ensureSeedTasks();
        const idx = tasks.findIndex(t => String(t.id) === String(taskId) || String(t.taskId || '') === String(taskId));
        if (idx === -1) return null;

        const current = tasks[idx];
        const next = typeof updater === 'function' ? updater({ ...current }) : { ...current, ...updater };
        tasks[idx] = next;
        this.saveTasks(tasks);
        return next;
    },

    acceptTask(taskId, runnerAuth = CommonUtils.getAuth() || {}, options = {}) {
        const tasks = this.ensureSeedTasks(runnerAuth);
        const idx = tasks.findIndex(t => String(t.id) === String(taskId) || String(t.taskId || '') === String(taskId));
        if (idx === -1) return { ok: false, message: '任务不存在或已下架' };

        const task = tasks[idx];
        if (task.status !== 'pending' || task.runnerId) {
            return { ok: false, message: '任务已被接单' };
        }

        const runnerId = runnerAuth.id || runnerAuth.openid || runnerAuth.name || 'runner';
        const runnerName = runnerAuth.name || runnerAuth.nickname || '跑腿员';

        const updated = {
            ...task,
            status: 'in-progress',
            progressStage: 'accepted',
            runnerId,
            runnerName,
            runner: runnerName,
            acceptTime: Date.now(),
            runnerConfirmed: false,
            publisherConfirmed: false,
            timeline: [...(task.timeline || []), { type: 'accepted', by: runnerId, time: Date.now() }]
        };

        tasks[idx] = updated;
        this.saveTasks(tasks);
        if (options.simulate) {
            this.simulateLifecycle(updated.id, options.simulateConfig);
        }
        return { ok: true, task: updated };
    },
    
    simulateLifecycle(taskId, config = {}) {
        const base = this.getSimulationConfig();
        const multiplier = Number(config.multiplier ?? base.multiplier ?? 1) || 1;
        const overrides = config.delays || {};
        const delays = {
            picking_up: overrides.picking_up ?? base.delays.picking_up,
            delivering: overrides.delivering ?? base.delays.delivering,
            delivered: overrides.delivered ?? base.delays.delivered,
            completed: overrides.completed ?? base.delays.completed
        };

        const steps = [
            { delay: delays.picking_up * multiplier, stage: 'picking_up' },
            { delay: delays.delivering * multiplier, stage: 'delivering' },
            { delay: delays.delivered * multiplier, stage: 'delivered' },
            { delay: delays.completed * multiplier, stage: 'completed', finalize: true }
        ];

        steps.forEach(step => {
            setTimeout(() => {
                this.updateTask(taskId, task => {
                    if (!task) return task;
                    if (task.status !== 'in-progress' && !step.finalize) return task;
                    if (!step.finalize && ['delivered', 'completed'].includes(task.progressStage)) return task;

                    if (step.finalize) {
                        if (task.status === 'completed') return task;
                        return {
                            ...task,
                            status: 'completed',
                            progressStage: 'completed',
                            runnerConfirmed: true,
                            publisherConfirmed: true,
                            completeTime: Date.now(),
                            timeline: [...(task.timeline || []), { type: 'completed', time: Date.now() }]
                        };
                    }

                    return {
                        ...task,
                        progressStage: step.stage,
                        timeline: [...(task.timeline || []), { type: step.stage, time: Date.now() }]
                    };
                });
            }, step.delay);
        });
    },

    getSimulationDurations(config = {}) {
        const base = this.getSimulationConfig();
        const multiplier = Number(config.multiplier || base.multiplier || 1) || 1;
        const overrides = config.delays || {};
        const delays = {
            picking_up: overrides.picking_up ?? base.delays.picking_up,
            delivering: overrides.delivering ?? base.delays.delivering,
            delivered: overrides.delivered ?? base.delays.delivered,
            completed: overrides.completed ?? base.delays.completed
        };

        const total = (delays.picking_up + delays.delivering + delays.delivered + delays.completed) * multiplier;
        return { total, delays, multiplier };
    },

    getSimulationConfig() {
        const key = 'taskSimConfig';
        try {
            const saved = localStorage.getItem(key);
            if (saved) return JSON.parse(saved);
        } catch (e) {
            console.warn('读取模拟配置失败，使用默认值', e);
        }
        return {
            multiplier: 1,
            delays: {
                picking_up: 2000,
                delivering: 5000,
                delivered: 8000,
                completed: 11000
            }
        };
    },

    setSimulationConfig(nextConfig = {}) {
        const key = 'taskSimConfig';
        const current = this.getSimulationConfig();
        const merged = {
            multiplier: typeof nextConfig.multiplier === 'number' ? nextConfig.multiplier : current.multiplier,
            delays: {
                ...current.delays,
                ...(nextConfig.delays || {})
            }
        };
        localStorage.setItem(key, JSON.stringify(merged));
        return merged;
    },

    startSimulationForPublisher(taskId, options = {}) {
        const auth = options.auth || CommonUtils.getAuth() || {};
        const tasks = this.ensureSeedTasks(auth);
        const idx = tasks.findIndex(t => String(t.id) === String(taskId) || String(t.taskId || '') === String(taskId));
        if (idx === -1) return { ok: false, message: '任务不存在或已下架' };

        const task = tasks[idx];
        if (task.status === 'completed') {
            return { ok: true, message: '任务已完成', task };
        }

        const simulatedRunner = this.getSimulatedRunnerProfile();
        const runnerId = task.runnerId || simulatedRunner.id;
        const runnerName = task.runnerName || task.runner || simulatedRunner.name;
        const runnerPhone = task.runnerPhone || simulatedRunner.phone;
        const alreadyAccepted = task.status === 'in-progress' || ['accepted', 'picking_up', 'delivering', 'delivered'].includes(task.progressStage);
        const timeline = task.timeline ? [...task.timeline] : [];
        if (!alreadyAccepted) {
            timeline.push({ type: 'accepted', by: runnerId, time: Date.now() });
        }

        const updated = {
            ...task,
            status: 'in-progress',
            progressStage: 'accepted',
            runnerId,
            runnerName,
            runner: runnerName,
            runnerPhone,
            acceptTime: task.acceptTime || Date.now(),
            timeline
        };

        tasks[idx] = updated;
        this.saveTasks(tasks);

        // 确保当前任务缓存也携带模拟跑腿员信息，便于评价页读取
        try {
            if (window.dataStore) {
                dataStore.set('currentTask', updated);
            }
        } catch (e) {
            console.warn('缓存模拟任务失败', e);
        }

        const simConfig = options.simulateConfig || this.getSimulationConfig();
        const durations = this.getSimulationDurations(simConfig);
        this.simulateLifecycle(taskId, simConfig);
        return { ok: true, task: updated, totalDuration: durations.total, delays: durations.delays };
    },

    markDelivered(taskId, runnerAuth = CommonUtils.getAuth() || {}) {
        return this.updateTask(taskId, task => {
            if (!task) return task;
            if (task.status !== 'in-progress') return task;
            const runnerId = runnerAuth.id || runnerAuth.openid || task.runnerId || 'runner';
            const runnerName = runnerAuth.name || runnerAuth.nickname || task.runnerName || task.runner || '跑腿员';

            return {
                ...task,
                runnerId,
                runnerName,
                runner: runnerName,
                progressStage: 'delivered',
                runnerConfirmed: true,
                timeline: [...(task.timeline || []), { type: 'delivered', by: runnerId, time: Date.now() }]
            };
        });
    },

    confirmCompletion(taskId, userAuth = CommonUtils.getAuth() || {}) {
        const userId = userAuth.id || userAuth.openid;
        return this.updateTask(taskId, task => {
            if (!task) return task;

            const isPublisher = !!userId && task.publisherId === userId;
            const isRunner = !!userId && task.runnerId === userId;

            const next = { ...task };
            if (isPublisher) next.publisherConfirmed = true;
            if (isRunner) next.runnerConfirmed = true;

            // 跑腿员可在配送中直接标记送达
            if (isRunner && task.status === 'in-progress' && task.progressStage !== 'delivered') {
                next.progressStage = 'delivered';
            }

            if (next.publisherConfirmed && next.runnerConfirmed) {
                next.status = 'completed';
                next.progressStage = 'completed';
                next.completeTime = Date.now();
                next.timeline = [...(next.timeline || []), { type: 'completed', time: Date.now() }];
            }

            return next;
        });
    },

    getTasksForUser(auth = CommonUtils.getAuth() || {}) {
        const ids = [auth.id, auth.openid, auth.name, auth.nickname, auth.phone].map(v => this.normalizeId(v)).filter(Boolean);
        const userId = ids[0];
        const tasks = (this.ensureSeedTasks(auth) || []).map(t => this.normalizeTaskIds(t));

        // 若补充了标准化字段，回写存储，避免后续页面再度缺失
        const storedTasks = dataStore.get('tasks') || [];
        const needsPersist = tasks.some((task, idx) => {
            const original = storedTasks[idx];
            if (!original) return true;
            return task.publisherId !== original.publisherId || task.runnerId !== original.runnerId || task.publisherName !== original.publisherName || task.runnerName !== original.runnerName;
        });
        if (needsPersist) {
            this.saveTasks(tasks);
        }

        if (ids.length === 0) return tasks;

        return tasks.filter(task => {
            const pubId = this.normalizeId(task.publisherId);
            const runnerId = this.normalizeId(task.runnerId);
            const pubName = this.normalizeId(task.publisherName || task.publisher);
            const runnerName = this.normalizeId(task.runnerName || task.runner);
            return ids.includes(pubId) || ids.includes(runnerId) || ids.includes(pubName) || ids.includes(runnerName);
        });
    },

    getAvailableTasks(auth = CommonUtils.getAuth() || {}) {
        const tasks = this.ensureSeedTasks(auth);
        return tasks.filter(task => task.status === 'pending');
    }
};
window.TaskFlow = TaskFlow;

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 更新用户状态显示
    stateManager.subscribe((state) => {
        const userInfoEl = document.querySelector('.user-info');
        if (userInfoEl && state.currentUser) {
            userInfoEl.innerHTML = `
                <span class="user-name">${state.currentUser.name}</span>
                <button class="login-btn" onclick="location.href='profile.html'">个人中心</button>
            `;
        }
    });

    // 初始化状态
    stateManager.init();
});

// 全局错误处理（仅提示一次，避免弹窗干扰）
let __globalErrorNotified = false;
window.addEventListener('error', function(e) {
    console.error('全局错误:', e.error);
    if (__globalErrorNotified) return;
    __globalErrorNotified = true;
    if (window.CommonUtils && CommonUtils.showToast) {
        CommonUtils.showToast('系统出现问题，请刷新后重试', 'error');
    }
});

// 页面卸载前保存状态
window.addEventListener('beforeunload', function() {
    stateManager.dataStore.set('appState', stateManager.state);
});