/* ===========================================
   微信小程序特有交互效果
   =========================================== */

// 微信小程序风格的下拉刷新
class WeChatPullToRefresh {
    constructor(container, onRefresh) {
        this.container = container;
        this.onRefresh = onRefresh;
        this.startY = 0;
        this.currentY = 0;
        this.refreshing = false;
        this.threshold = 80;
        this.refreshIndicator = null;
        
        // 性能优化：节流处理
        this.throttledOnTouchMove = this.throttle(this.onTouchMove.bind(this), 16);
        
        this.init();
    }
    
    // 节流函数 - 性能优化
    throttle(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
    
    init() {
        // 创建刷新指示器
        this.createRefreshIndicator();
        
        // 绑定触摸事件 - 使用被动事件监听器提升滚动性能
        this.container.addEventListener('touchstart', this.onTouchStart.bind(this), { passive: false });
        this.container.addEventListener('touchmove', this.throttledOnTouchMove, { passive: false });
        this.container.addEventListener('touchend', this.onTouchEnd.bind(this), { passive: true });
    }
    
    createRefreshIndicator() {
        this.indicator = document.createElement('div');
        this.indicator.className = 'refresh-indicator';
        this.indicator.innerHTML = `
            <div class="refresh-content">
                <div class="refresh-icon">↓</div>
                <div class="refresh-text">下拉刷新</div>
            </div>
        `;
        this.container.parentNode.insertBefore(this.indicator, this.container);
    }
    
    onTouchStart(e) {
        if (this.refreshing || window.scrollY > 0) return;
        
        this.startY = e.touches[0].pageY;
        this.currentY = this.startY;
    }
    
    onTouchMove(e) {
        if (this.refreshing || window.scrollY > 0) return;
        
        this.currentY = e.touches[0].pageY;
        const diff = this.currentY - this.startY;
        
        if (diff > 0) {
            e.preventDefault();
            this.updateRefreshIndicator(diff);
        }
    }
    
    onTouchEnd(e) {
        if (this.refreshing || window.scrollY > 0) return;
        
        const diff = this.currentY - this.startY;
        
        if (diff > this.threshold) {
            this.startRefresh();
        } else {
            this.resetRefreshIndicator();
        }
    }
    
    updateRefreshIndicator(diff) {
        const progress = Math.min(diff / this.threshold, 1);
        const translateY = Math.min(diff * 0.5, this.threshold);
        
        this.indicator.style.transform = `translateY(${translateY}px)`;
        
        if (diff > this.threshold) {
            this.indicator.querySelector('.refresh-text').textContent = '释放刷新';
            this.indicator.querySelector('.refresh-icon').textContent = '↑';
        } else {
            this.indicator.querySelector('.refresh-text').textContent = '下拉刷新';
            this.indicator.querySelector('.refresh-icon').textContent = '↓';
        }
    }
    
    startRefresh() {
        this.refreshing = true;
        this.indicator.querySelector('.refresh-text').textContent = '刷新中...';
        this.indicator.querySelector('.refresh-icon').textContent = '⟳';
        
        // 执行刷新回调
        if (this.onRefresh) {
            this.onRefresh().then(() => {
                this.finishRefresh();
            }).catch(() => {
                this.finishRefresh();
            });
        } else {
            setTimeout(() => {
                this.finishRefresh();
            }, 1000);
        }
    }
    
    finishRefresh() {
        this.refreshing = false;
        this.resetRefreshIndicator();
    }
    
    resetRefreshIndicator() {
        this.indicator.style.transform = 'translateY(0)';
        setTimeout(() => {
            this.indicator.querySelector('.refresh-text').textContent = '下拉刷新';
            this.indicator.querySelector('.refresh-icon').textContent = '↓';
        }, 300);
    }
}

// 微信小程序风格的上拉加载更多
class WeChatLoadMore {
    constructor(container, onLoadMore) {
        this.container = container;
        this.onLoadMore = onLoadMore;
        this.loading = false;
        this.hasMore = true;
        
        // 性能优化：防抖处理
        this.debouncedOnScroll = this.debounce(this.onScroll.bind(this), 100);
        
        this.init();
    }
    
    // 防抖函数 - 性能优化
    debounce(func, wait) {
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
    
    init() {
        // 创建加载更多指示器
        this.createLoadMoreIndicator();
        
        // 监听滚动事件 - 使用防抖优化性能
        window.addEventListener('scroll', this.debouncedOnScroll, { passive: true });
    }
    
    createLoadMoreIndicator() {
        this.indicator = document.createElement('div');
        this.indicator.className = 'load-more-indicator';
        this.indicator.innerHTML = `
            <div class="load-more-content">
                <div class="load-more-text">上拉加载更多</div>
            </div>
        `;
        this.container.appendChild(this.indicator);
    }
    
    onScroll() {
        if (this.loading || !this.hasMore) return;
        
        const scrollTop = window.scrollY || document.documentElement.scrollTop;
        const windowHeight = window.innerHeight;
        const documentHeight = document.documentElement.scrollHeight;
        
        // 距离底部50px时触发加载
        if (scrollTop + windowHeight >= documentHeight - 50) {
            this.startLoadMore();
        }
    }
    
    startLoadMore() {
        this.loading = true;
        this.indicator.querySelector('.load-more-text').textContent = '加载中...';
        
        if (this.onLoadMore) {
            this.onLoadMore().then((hasMore) => {
                this.finishLoadMore(hasMore);
            }).catch(() => {
                this.finishLoadMore(true);
            });
        } else {
            setTimeout(() => {
                this.finishLoadMore(true);
            }, 1000);
        }
    }
    
    finishLoadMore(hasMore) {
        this.loading = false;
        this.hasMore = hasMore !== false;
        
        if (this.hasMore) {
            this.indicator.querySelector('.load-more-text').textContent = '上拉加载更多';
        } else {
            this.indicator.querySelector('.load-more-text').textContent = '没有更多了';
        }
    }
}

// 微信小程序风格的页面切换动画
class WeChatPageTransition {
    static navigateTo(url) {
        // 创建页面切换遮罩
        const overlay = document.createElement('div');
        overlay.className = 'page-transition-overlay';
        document.body.appendChild(overlay);
        
        // 添加动画效果
        overlay.style.opacity = '1';
        
        setTimeout(() => {
            window.location.href = url;
        }, 300);
    }
    
    static navigateBack() {
        // 创建页面返回遮罩
        const overlay = document.createElement('div');
        overlay.className = 'page-transition-overlay back';
        document.body.appendChild(overlay);
        
        // 添加动画效果
        overlay.style.opacity = '1';
        
        setTimeout(() => {
            // 检查历史记录长度，如果小于等于1，跳转到首页，否则返回上一页
            if (window.history.length <= 1) {
                window.location.href = '首页.html';
            } else {
                window.history.back();
            }
        }, 300);
    }
}

// 微信小程序风格的模态框
class WeChatModal {
    constructor(options = {}) {
        this.title = options.title || '提示';
        this.content = options.content || '';
        this.showCancel = options.showCancel !== false;
        this.cancelText = options.cancelText || '取消';
        this.confirmText = options.confirmText || '确定';
        
        this.createModal();
    }
    
    createModal() {
        this.modal = document.createElement('div');
        this.modal.className = 'wechat-modal';
        this.modal.innerHTML = `
            <div class="modal-mask"></div>
            <div class="modal-content">
                <div class="modal-header">
                    <div class="modal-title">${this.title}</div>
                </div>
                <div class="modal-body">
                    <div class="modal-text">${this.content}</div>
                </div>
                <div class="modal-footer">
                    ${this.showCancel ? `<button class="modal-btn modal-cancel">${this.cancelText}</button>` : ''}
                    <button class="modal-btn modal-confirm">${this.confirmText}</button>
                </div>
            </div>
        `;
        
        document.body.appendChild(this.modal);
        this.bindEvents();
        this.show();
    }
    
    bindEvents() {
        const mask = this.modal.querySelector('.modal-mask');
        const cancelBtn = this.modal.querySelector('.modal-cancel');
        const confirmBtn = this.modal.querySelector('.modal-confirm');
        
        mask?.addEventListener('click', () => this.hide());
        cancelBtn?.addEventListener('click', () => this.hide());
        confirmBtn?.addEventListener('click', () => {
            this.hide();
            if (this.onConfirm) this.onConfirm();
        });
    }
    
    show() {
        setTimeout(() => {
            this.modal.classList.add('show');
        }, 10);
    }
    
    hide() {
        this.modal.classList.remove('show');
        setTimeout(() => {
            if (this.modal.parentNode) {
                this.modal.parentNode.removeChild(this.modal);
            }
        }, 300);
    }
    
    then(callback) {
        this.onConfirm = callback;
        return this;
    }
}

// 微信小程序风格的轻提示
class WeChatToast {
    static show(options) {
        const message = typeof options === 'string' ? options : options.message;
        const duration = options.duration || 2000;
        
        const toast = document.createElement('div');
        toast.className = 'wechat-toast';
        toast.textContent = message;
        
        document.body.appendChild(toast);
        
        // 显示动画
        setTimeout(() => {
            toast.classList.add('show');
        }, 10);
        
        // 自动隐藏
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, duration);
    }
}

// 微信小程序风格的加载中提示
class WeChatLoading {
    static show(options) {
        const title = options?.title || '加载中...';
        
        this.loading = document.createElement('div');
        this.loading.className = 'wechat-loading';
        this.loading.innerHTML = `
            <div class="loading-mask"></div>
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <div class="loading-text">${title}</div>
            </div>
        `;
        
        document.body.appendChild(this.loading);
        
        setTimeout(() => {
            this.loading.classList.add('show');
        }, 10);
    }
    
    static hide() {
        if (this.loading) {
            this.loading.classList.remove('show');
            setTimeout(() => {
                if (this.loading.parentNode) {
                    this.loading.parentNode.removeChild(this.loading);
                }
                this.loading = null;
            }, 300);
        }
    }
}

// 全局初始化微信小程序交互效果
function initWeChatInteractions() {
    // 为所有链接添加页面切换动画
    document.addEventListener('click', function(e) {
        const link = e.target.closest('a[href]');
        if (link && link.getAttribute('href').endsWith('.html')) {
            e.preventDefault();
            WeChatPageTransition.navigateTo(link.getAttribute('href'));
        }
    });
    
    // 为返回按钮添加返回动画
    document.addEventListener('click', function(e) {
        if (e.target.closest('.back-btn') || e.target.closest('[onclick*="goBack"]')) {
            e.preventDefault();
            WeChatPageTransition.navigateBack();
        }
    });
    
    // 替换原生alert和confirm
    window.originalAlert = window.alert;
    window.originalConfirm = window.confirm;
    
    window.alert = function(message) {
        return new WeChatModal({
            title: '提示',
            content: message,
            showCancel: false,
            confirmText: '确定'
        });
    };
    
    window.confirm = function(message) {
        return new Promise((resolve) => {
            new WeChatModal({
                title: '确认',
                content: message,
                showCancel: true,
                cancelText: '取消',
                confirmText: '确定'
            }).then(() => resolve(true));
        });
    };
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    initWeChatInteractions();
});