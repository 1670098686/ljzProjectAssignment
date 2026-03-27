// 错误处理管理器 - 统一管理所有错误的捕获和提示
class ErrorManager {
    constructor() {
        // 错误类型配置
        this.errorTypes = {
            // 网络错误
            network: {
                code: 'NETWORK_ERROR',
                message: '网络连接失败，请检查网络设置后重试',
                level: 'error'
            },
            // 服务器错误
            server: {
                code: 'SERVER_ERROR',
                message: '服务器繁忙，请稍后再试',
                level: 'error'
            },
            // 权限错误
            permission: {
                code: 'PERMISSION_ERROR',
                message: '您没有权限执行此操作',
                level: 'warning'
            },
            // 登录错误
            login: {
                code: 'LOGIN_ERROR',
                message: '请先登录后再执行此操作',
                level: 'warning'
            },
            // 表单验证错误
            validation: {
                code: 'VALIDATION_ERROR',
                message: '表单验证失败，请检查输入内容',
                level: 'warning'
            },
            // 数据错误
            data: {
                code: 'DATA_ERROR',
                message: '数据处理失败，请稍后再试',
                level: 'error'
            },
            // 未知错误
            unknown: {
                code: 'UNKNOWN_ERROR',
                message: '发生了未知错误，请稍后再试',
                level: 'error'
            }
        };
        
        // 默认配置
        this.defaultConfig = {
            type: 'toast', // toast, alert, modal
            duration: 3000,
            level: 'error', // error, warning, info, success
            title: '提示',
            showStackTrace: false,
            logToConsole: true
        };
        
        // 初始化全局错误监听
        this.initGlobalErrorListener();
    }
    
    // 初始化全局错误监听
    initGlobalErrorListener() {
        // 监听全局JS错误
        window.addEventListener('error', (event) => {
            this.handleError(event.error || event.message, {
                type: 'unknown',
                source: 'global',
                filename: event.filename,
                lineno: event.lineno,
                colno: event.colno
            });
        });
        
        // 监听未捕获的Promise错误
        window.addEventListener('unhandledrejection', (event) => {
            this.handleError(event.reason || 'Unhandled Promise Rejection', {
                type: 'unknown',
                source: 'promise'
            });
        });
    }
    
    // 处理错误
    handleError(error, options = {}) {
        const finalConfig = { ...this.defaultConfig, ...options };
        
        // 格式化错误信息
        const errorInfo = this.formatError(error, finalConfig);
        
        // 记录错误日志
        if (finalConfig.logToConsole) {
            this.logError(errorInfo);
        }
        
        // 显示错误提示
        this.showError(errorInfo, finalConfig);
        
        return errorInfo;
    }
    
    // 格式化错误信息
    formatError(error, config) {
        let errorInfo = {
            code: config.code || 'UNKNOWN_ERROR',
            message: config.message || '发生了未知错误',
            level: config.level,
            timestamp: new Date().toISOString(),
            source: config.source || 'unknown'
        };
        
        // 如果是字符串错误
        if (typeof error === 'string') {
            errorInfo.message = error;
        }
        // 如果是Error对象
        else if (error instanceof Error) {
            errorInfo.message = error.message || this.errorTypes.unknown.message;
            errorInfo.stack = error.stack;
        }
        // 如果是自定义错误对象
        else if (error && typeof error === 'object') {
            errorInfo = { ...errorInfo, ...error };
        }
        
        return errorInfo;
    }
    
    // 记录错误日志
    logError(errorInfo) {
        console[errorInfo.level](`[${errorInfo.timestamp}] ${errorInfo.code}: ${errorInfo.message}`);
        
        // 显示堆栈跟踪
        if (errorInfo.stack && this.defaultConfig.showStackTrace) {
            console[errorInfo.level](errorInfo.stack);
        }
    }
    
    // 显示错误提示
    showError(errorInfo, config) {
        switch (config.type) {
            case 'toast':
                this.showToast(errorInfo, config);
                break;
            case 'alert':
                this.showAlert(errorInfo, config);
                break;
            case 'modal':
                this.showModal(errorInfo, config);
                break;
            default:
                this.showToast(errorInfo, config);
        }
    }
    
    // 显示Toast提示
    showToast(errorInfo, config) {
        // 如果CommonUtils已经有showToast方法，直接使用
        if (typeof CommonUtils !== 'undefined' && CommonUtils.showToast) {
            CommonUtils.showToast(errorInfo.message, errorInfo.level, config.duration);
        } else {
            // 否则使用简单的Toast实现
            const toast = document.createElement('div');
            toast.className = `error-toast toast-${errorInfo.level}`;
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
                opacity: 0;
                transition: opacity 0.3s ease;
            `;
            
            // 根据错误级别设置背景色
            if (errorInfo.level === 'success') {
                toast.style.background = '#52c41a';
            } else if (errorInfo.level === 'warning') {
                toast.style.background = '#faad14';
            } else if (errorInfo.level === 'error') {
                toast.style.background = '#ff4d4f';
            } else if (errorInfo.level === 'info') {
                toast.style.background = '#1890ff';
            }
            
            toast.textContent = errorInfo.message;
            document.body.appendChild(toast);
            
            // 显示Toast
            setTimeout(() => {
                toast.style.opacity = '1';
            }, 100);
            
            // 隐藏Toast
            setTimeout(() => {
                toast.style.opacity = '0';
                setTimeout(() => {
                    if (toast.parentElement) {
                        toast.parentElement.removeChild(toast);
                    }
                }, 300);
            }, config.duration);
        }
    }
    
    // 显示Alert提示
    showAlert(errorInfo, config) {
        alert(`${config.title}: ${errorInfo.message}`);
    }
    
    // 显示Modal提示
    showModal(errorInfo, config) {
        // 如果ModalManager已经有showAlert方法，直接使用
        if (typeof modalManager !== 'undefined' && modalManager.showAlert) {
            modalManager.showAlert(config.title, errorInfo.message, errorInfo.level);
        } else {
            // 否则使用简单的Modal实现
            const modal = document.createElement('div');
            modal.className = 'error-modal';
            modal.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 9999;
            `;
            
            const modalContent = document.createElement('div');
            modalContent.style.cssText = `
                background: white;
                padding: 24px;
                border-radius: 8px;
                max-width: 400px;
                width: 90%;
            `;
            
            const modalTitle = document.createElement('h3');
            modalTitle.style.cssText = `
                margin: 0 0 16px 0;
                font-size: 18px;
                font-weight: 600;
            `;
            modalTitle.textContent = config.title;
            
            const modalBody = document.createElement('p');
            modalBody.style.cssText = `
                margin: 0 0 20px 0;
                color: #333;
                line-height: 1.5;
            `;
            modalBody.textContent = errorInfo.message;
            
            const modalFooter = document.createElement('div');
            modalFooter.style.cssText = `
                display: flex;
                justify-content: flex-end;
            `;
            
            const closeBtn = document.createElement('button');
            closeBtn.style.cssText = `
                padding: 8px 16px;
                background: #1890ff;
                color: white;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                font-size: 14px;
            `;
            closeBtn.textContent = '确定';
            closeBtn.onclick = () => {
                document.body.removeChild(modal);
            };
            
            modalFooter.appendChild(closeBtn);
            modalContent.appendChild(modalTitle);
            modalContent.appendChild(modalBody);
            modalContent.appendChild(modalFooter);
            modal.appendChild(modalContent);
            
            document.body.appendChild(modal);
        }
    }
    
    // 根据错误类型获取错误信息
    getErrorByType(type, customMessage = '') {
        const errorConfig = this.errorTypes[type] || this.errorTypes.unknown;
        return {
            ...errorConfig,
            message: customMessage || errorConfig.message
        };
    }
    
    // 快速处理错误（简化调用）
    static quickHandle(error, options = {}) {
        // 确保errorManager已初始化
        if (!window.errorManager) {
            window.errorManager = new ErrorManager();
        }
        return window.errorManager.handleError(error, options);
    }
    
    // 验证错误处理
    handleValidationError(errors, config = {}) {
        const validationError = this.getErrorByType('validation', '表单验证失败，请检查输入内容');
        
        const errorInfo = {
            ...validationError,
            errors: Array.isArray(errors) ? errors : [errors],
            message: Array.isArray(errors) ? errors.join('\n') : errors
        };
        
        this.handleError(errorInfo, { ...config, type: 'toast' });
        return errorInfo;
    }
    
    // 网络错误处理
    handleNetworkError(error, config = {}) {
        const networkError = this.getErrorByType('network');
        this.handleError({
            ...networkError,
            originalError: error
        }, config);
    }
    
    // 权限错误处理
    handlePermissionError(config = {}) {
        const permissionError = this.getErrorByType('permission');
        this.handleError(permissionError, config);
    }
    
    // 登录错误处理
    handleLoginError(config = {}) {
        const loginError = this.getErrorByType('login');
        this.handleError(loginError, config);
        
        // 自动跳转到登录页
        if (window.router) {
            setTimeout(() => {
                window.router.navigate('login');
            }, 1500);
        }
    }
}

// 创建全局错误处理管理器实例
window.errorManager = new ErrorManager();

// 扩展CommonUtils，添加错误处理方法
if (typeof CommonUtils !== 'undefined') {
    CommonUtils.handleError = ErrorManager.quickHandle;
    CommonUtils.handleValidationError = (errors, config = {}) => {
        if (window.errorManager) {
            window.errorManager.handleValidationError(errors, config);
        }
    };
    CommonUtils.handleNetworkError = (error, config = {}) => {
        if (window.errorManager) {
            window.errorManager.handleNetworkError(error, config);
        }
    };
    CommonUtils.handlePermissionError = (config = {}) => {
        if (window.errorManager) {
            window.errorManager.handlePermissionError(config);
        }
    };
    CommonUtils.handleLoginError = (config = {}) => {
        if (window.errorManager) {
            window.errorManager.handleLoginError(config);
        }
    };
}
