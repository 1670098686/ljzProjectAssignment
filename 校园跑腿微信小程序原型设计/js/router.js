// 路由管理器 - 统一管理所有页面的路由配置和跳转逻辑

// 兜底防护：当 common.js 未成功加载时，提供最小可用的 CommonUtils，防止脚本直接报错
if (typeof window.CommonUtils === 'undefined') {
    window.CommonUtils = {
        getAuth: () => null,
        generateId: () => Date.now().toString(36) + Math.random().toString(36).slice(2),
        requireRunnerAccess: () => true,
        requireAdminAccess: () => true
    };
    console.warn('CommonUtils 未加载，router.js 正在使用降级实现');
}

class Router {
    constructor() {
        // 路由配置表
        this.routes = {
            // 基础页面
            'login': {
                path: '登录.html',
                requiresAuth: false,
                requiresIdentity: false
            },
            'phoneBind': {
                path: '手机绑定.html',
                requiresAuth: false,
                requiresIdentity: false
            },
            'identitySelect': {
                path: '身份选择.html',
                requiresAuth: true,
                requiresIdentity: false
            },
            'home': {
                path: '首页.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'taskList': {
                path: '任务列表.html',
                requiresAuth: true,
                requiresIdentity: true,
                requiresRunner: true
            },
            'taskPublish': {
                path: '任务发布.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'taskDetail': {
                path: '任务详情.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'taskStatus': {
                path: '任务状态.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'taskRecord': {
                path: '任务记录.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'messages': {
                path: '消息.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'messageDetail': {
                path: '消息对话.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'profile': {
                path: '个人中心.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'creditDetail': {
                path: '信用详情.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'about': {
                path: '关于.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'settings': {
                path: '安全设置.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'realNameAuth': {
                path: '实名认证.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'runnerVerification': {
                path: '跑腿员验证.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'payment': {
                path: '支付确认.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'admin': {
                path: '后台管理.html',
                requiresAuth: true,
                requiresIdentity: true,
                requiresAdmin: true
            },
            'feedback': {
                path: '意见反馈.html',
                requiresAuth: true,
                requiresIdentity: true
            },
            'demoAccounts': {
                path: 'demo_accounts.html',
                requiresAuth: false,
                requiresIdentity: false
            }
        };
        
        // 当前路由
        this.currentRoute = null;
        
        // 初始化路由
        this.init();
    }
    
    // 初始化路由
    init() {
        // 获取当前页面路径
        const currentPath = window.location.pathname.split('/').pop();
        
        // 查找当前路由
        for (const [routeName, config] of Object.entries(this.routes)) {
            if (config.path === currentPath) {
                this.currentRoute = routeName;
                break;
            }
        }
        
        // 页面加载完成后检查权限
        document.addEventListener('DOMContentLoaded', () => {
            this.checkPermissions(this.currentRoute);
        });
    }
    
    // 检查权限
    checkPermissions(routeName) {
        const route = this.routes[routeName];
        if (!route) return;
        
        const auth = CommonUtils.getAuth();
        const isLoggedIn = auth && auth.loggedIn;
        const hasIdentity = auth && auth.role;
        
        // 检查登录状态
        if (route.requiresAuth && !isLoggedIn) {
            this.navigate('login');
            return false;
        }
        
        // 检查身份选择
        if (route.requiresIdentity && !hasIdentity) {
            this.navigate('identitySelect');
            return false;
        }
        
        // 检查跑腿员权限
        if (route.requiresRunner) {
            if (!CommonUtils.requireRunnerAccess({ redirectIfNoRunner: '首页.html' })) {
                return false;
            }
        }
        
        // 检查管理员权限
        if (route.requiresAdmin) {
            if (!CommonUtils.requireAdminAccess('首页.html')) {
                return false;
            }
        }
        
        return true;
    }
    
    // 页面跳转
    navigate(routeName, params = {}) {
        const route = this.routes[routeName];
        if (!route) {
            console.error(`路由 ${routeName} 不存在`);
            return false;
        }
        
        // 检查权限
        if (!this.checkPermissions(routeName)) {
            return false;
        }
        
        // 构建完整路径，包含参数
        const fullPath = this.buildUrl(route.path, params);
        
        // 执行跳转
        window.location.href = fullPath;
        return true;
    }
    
    // 构建带参数的URL
    buildUrl(path, params) {
        if (Object.keys(params).length === 0) {
            return path;
        }
        
        const queryString = this.encodeParams(params);
        return `${path}?${queryString}`;
    }
    
    // 编码路由参数
    encodeParams(params) {
        return Object.entries(params)
            .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
            .join('&');
    }
    
    // 解码路由参数
    decodeParams(queryString) {
        if (!queryString || queryString === '') {
            return {};
        }
        
        // 移除开头的问号
        if (queryString.startsWith('?')) {
            queryString = queryString.slice(1);
        }
        
        return queryString.split('&')
            .map(param => param.split('='))
            .reduce((acc, [key, value]) => {
                if (key) {
                    acc[decodeURIComponent(key)] = decodeURIComponent(value || '');
                }
                return acc;
            }, {});
    }
    
    // 获取当前页面的路由参数
    getCurrentParams() {
        const queryString = window.location.search;
        return this.decodeParams(queryString);
    }
    
    // 刷新当前页面
    refresh() {
        window.location.reload();
    }
    
    // 后退
    back() {
        window.history.back();
    }
    
    // 获取当前路由名称
    getCurrentRoute() {
        return this.currentRoute;
    }
    
    // 获取当前页面路径
    getCurrentPath() {
        return window.location.pathname;
    }
    
    // 检查当前页面是否是某个路由
    isCurrentRoute(routeName) {
        return this.currentRoute === routeName;
    }
}

// 创建全局路由实例（类定义完成后再实例化，避免未定义报错）
if (!window.router) {
    window.router = new Router();
}
