// 加载状态管理器 - 统一管理所有异步操作的加载提示
class LoadManager {
    constructor() {
        this.loadingCount = 0;
        this.loadingElement = null;
        this.localLoadingElements = new Map();
        
        // 默认配置
        this.defaultConfig = {
            text: '加载中...',
            timeout: 10000, // 10秒超时
            fullscreen: true,
            theme: 'default' // default, light, dark
        };
        
        // 初始化全局加载元素
        this.initLoadingElement();
    }
    
    // 初始化全局加载元素
    initLoadingElement() {
        this.loadingElement = document.createElement('div');
        this.loadingElement.id = 'globalLoading';
        this.loadingElement.className = 'loading-container';
        this.loadingElement.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(255, 255, 255, 0.8);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            opacity: 0;
            visibility: hidden;
            transition: opacity 0.3s ease, visibility 0.3s ease;
        `;
        
        // 创建加载动画
        const spinner = document.createElement('div');
        spinner.className = 'loading-spinner';
        spinner.style.cssText = `
            width: 48px;
            height: 48px;
            border: 4px solid #f3f3f3;
            border-top: 4px solid #1890ff;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-bottom: 16px;
        `;
        
        // 创建加载文字
        const text = document.createElement('div');
        text.className = 'loading-text';
        text.style.cssText = `
            color: #333;
            font-size: 14px;
            font-weight: 500;
        `;
        text.textContent = this.defaultConfig.text;
        
        // 添加到加载容器
        this.loadingElement.appendChild(spinner);
        this.loadingElement.appendChild(text);
        
        // 添加到body
        document.body.appendChild(this.loadingElement);
        
        // 添加动画样式
        const style = document.createElement('style');
        style.textContent = `
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
        `;
        document.head.appendChild(style);
    }
    
    // 显示加载提示
    show(config = {}) {
        const finalConfig = { ...this.defaultConfig, ...config };
        
        if (finalConfig.fullscreen) {
            return this.showFullscreen(finalConfig);
        } else {
            return this.showLocal(finalConfig);
        }
    }
    
    // 显示全屏加载
    showFullscreen(config) {
        this.loadingCount++;
        
        // 更新加载文字
        const textElement = this.loadingElement.querySelector('.loading-text');
        if (textElement) {
            textElement.textContent = config.text;
        }
        
        // 显示加载容器
        this.loadingElement.style.opacity = '1';
        this.loadingElement.style.visibility = 'visible';
        
        // 设置超时
        let timeoutId;
        if (config.timeout > 0) {
            timeoutId = setTimeout(() => {
                this.hide();
                console.error('加载超时');
            }, config.timeout);
        }
        
        // 返回隐藏函数
        return {
            hide: () => {
                if (timeoutId) {
                    clearTimeout(timeoutId);
                }
                this.hide();
            }
        };
    }
    
    // 显示局部加载
    showLocal(config) {
        if (!config.target) {
            console.error('局部加载需要指定target元素');
            return { hide: () => {} };
        }
        
        const target = typeof config.target === 'string' ? document.querySelector(config.target) : config.target;
        if (!target) {
            console.error('找不到指定的target元素');
            return { hide: () => {} };
        }
        
        // 创建局部加载元素
        const localLoading = document.createElement('div');
        localLoading.className = 'local-loading';
        localLoading.style.cssText = `
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(255, 255, 255, 0.9);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            border-radius: inherit;
            z-index: 1000;
        `;
        
        // 创建加载动画
        const spinner = document.createElement('div');
        spinner.className = 'loading-spinner';
        spinner.style.cssText = `
            width: 32px;
            height: 32px;
            border: 3px solid #f3f3f3;
            border-top: 3px solid #1890ff;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-bottom: 8px;
        `;
        
        // 创建加载文字
        const text = document.createElement('div');
        text.className = 'loading-text';
        text.style.cssText = `
            color: #333;
            font-size: 12px;
            font-weight: 500;
        `;
        text.textContent = config.text;
        
        // 添加到局部加载容器
        localLoading.appendChild(spinner);
        localLoading.appendChild(text);
        
        // 保存局部加载元素
        const loadingKey = Date.now().toString();
        this.localLoadingElements.set(loadingKey, localLoading);
        
        // 设置目标元素为相对定位
        if (window.getComputedStyle(target).position === 'static') {
            target.style.position = 'relative';
        }
        
        // 添加到目标元素
        target.appendChild(localLoading);
        
        // 设置超时
        let timeoutId;
        if (config.timeout > 0) {
            timeoutId = setTimeout(() => {
                this.hideLocal(loadingKey);
                console.error('局部加载超时');
            }, config.timeout);
        }
        
        // 返回隐藏函数
        return {
            hide: () => {
                if (timeoutId) {
                    clearTimeout(timeoutId);
                }
                this.hideLocal(loadingKey);
            }
        };
    }
    
    // 隐藏加载提示
    hide() {
        this.loadingCount--;
        
        // 当所有加载完成后隐藏
        if (this.loadingCount <= 0) {
            this.loadingCount = 0;
            this.loadingElement.style.opacity = '0';
            this.loadingElement.style.visibility = 'hidden';
        }
    }
    
    // 隐藏局部加载
    hideLocal(loadingKey) {
        const localLoading = this.localLoadingElements.get(loadingKey);
        if (localLoading) {
            // 平滑隐藏
            localLoading.style.transition = 'opacity 0.3s ease';
            localLoading.style.opacity = '0';
            
            setTimeout(() => {
                if (localLoading.parentElement) {
                    localLoading.parentElement.removeChild(localLoading);
                }
                this.localLoadingElements.delete(loadingKey);
            }, 300);
        }
    }
    
    // 快速显示加载提示（简化调用）
    static quickShow(config = {}) {
        // 确保loadManager已初始化
        if (!window.loadManager) {
            window.loadManager = new LoadManager();
        }
        return window.loadManager.show(config);
    }
    
    // 异步函数包装器，自动添加加载提示
    wrapAsyncFunction(func, config = {}) {
        return async (...args) => {
            const loader = this.show(config);
            try {
                const result = await func(...args);
                loader.hide();
                return result;
            } catch (error) {
                loader.hide();
                console.error('异步操作失败:', error);
                throw error;
            }
        };
    }
}

// 创建全局加载状态管理器实例
window.loadManager = new LoadManager();

// 扩展CommonUtils，添加加载管理方法
if (typeof CommonUtils !== 'undefined') {
    CommonUtils.showLoading = LoadManager.quickShow;
    CommonUtils.hideLoading = () => {
        if (window.loadManager) {
            window.loadManager.hide();
        }
    };
    
    // 包装异步函数，自动添加加载提示
    CommonUtils.wrapAsync = (func, config) => {
        return window.loadManager.wrapAsyncFunction(func, config);
    };
}