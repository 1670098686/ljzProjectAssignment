// 首页功能实现

// 轮播图功能
class Carousel {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.items = this.container.querySelectorAll('.carousel-item');
        this.indicators = this.container.querySelectorAll('.indicator');
        this.currentIndex = 0;
        this.interval = null;
        this.init();
    }

    init() {
        // 启动自动轮播
        this.startAutoPlay();
        
        // 添加触摸事件支持
        this.addTouchEvents();
        
        // 添加键盘事件支持
        this.addKeyboardEvents();
    }

    startAutoPlay() {
        this.interval = setInterval(() => {
            this.next();
        }, 3000);
    }

    stopAutoPlay() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
    }

    next() {
        this.goToSlide((this.currentIndex + 1) % this.items.length);
    }

    prev() {
        this.goToSlide((this.currentIndex - 1 + this.items.length) % this.items.length);
    }

    goToSlide(index) {
        // 移除当前激活状态
        this.items[this.currentIndex].classList.remove('active');
        this.indicators[this.currentIndex].classList.remove('active');
        
        // 设置新的激活状态
        this.currentIndex = index;
        this.items[this.currentIndex].classList.add('active');
        this.indicators[this.currentIndex].classList.add('active');
    }

    addTouchEvents() {
        let startX = 0;
        let endX = 0;
        
        this.container.addEventListener('touchstart', (e) => {
            startX = e.touches[0].clientX;
            this.stopAutoPlay();
        });
        
        this.container.addEventListener('touchmove', (e) => {
            endX = e.touches[0].clientX;
        });
        
        this.container.addEventListener('touchend', () => {
            const diff = startX - endX;
            if (Math.abs(diff) > 50) { // 滑动距离阈值
                if (diff > 0) {
                    this.next();
                } else {
                    this.prev();
                }
            }
            this.startAutoPlay();
        });
    }

    addKeyboardEvents() {
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowLeft') {
                this.prev();
            } else if (e.key === 'ArrowRight') {
                this.next();
            }
        });
    }
}

// 热门任务管理器
class HotTaskManager {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.init();
    }

    init() {
        this.renderTasks();
        
        // 监听状态变化
        stateManager.subscribe((state) => {
            this.renderTasks();
        });
    }

    renderTasks() {
        const tasks = stateManager.state.tasks.slice(0, 3); // 只显示前3个任务
        
        if (tasks.length === 0) {
            this.container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon"><img src="images/icon-package.svg" class="icon-img"></div>
                    <div class="empty-text">暂无热门任务</div>
                    <div class="empty-desc">快去发布第一个任务吧！</div>
                </div>
            `;
            return;
        }

        this.container.innerHTML = tasks.map(task => this.createTaskCard(task)).join('');
    }

    createTaskCard(task) {
        const urgentClass = task.urgent ? 'urgent' : '';
        const statusText = this.getStatusText(task.status);
        const statusClass = this.getStatusClass(task.status);
        
        return `
            <div class="task-card ${urgentClass}" onclick="viewTaskDetail(${task.id})">
                <div class="task-header">
                    <div class="task-type">
                        <span class="task-icon">${this.getTaskIcon(task.type)}</span>
                        <span class="task-title">${task.title}</span>
                    </div>
                    <div class="task-price">${CommonUtils.formatPrice(task.price)}</div>
                </div>
                <div class="task-info">
                    <div class="task-location">
                        <img src="images/icon-location.svg" class="icon-img"> ${task.from} → ${task.to}
                    </div>
                    <div class="task-time">
                        <img src="images/icon-clock.svg" class="icon-img"> ${CommonUtils.formatTime(task.createTime)}
                    </div>
                </div>
                <div class="task-footer">
                    <span class="task-status ${statusClass}">${statusText}</span>
                    <span class="task-publisher">${task.publisher}</span>
                </div>
            </div>
        `;
    }

    getTaskIcon(type) {
        const icons = {
            '快递': '<img src="images/icon-package.svg" class="icon-img">',
            '餐食': '<img src="images/icon-food.svg" class="icon-img">',
            '文件': '<img src="images/icon-document.svg" class="icon-img">',
            '其他': '<img src="images/icon-other.svg" class="icon-img">'
        };
        return icons[type] || '<img src="images/icon-tasks.svg" class="icon-img">';
    }

    getStatusText(status) {
        const statusMap = {
            'pending': '待接单',
            'in-progress': '进行中',
            'completed': '已完成'
        };
        return statusMap[status] || '未知';
    }

    getStatusClass(status) {
        const classMap = {
            'pending': 'status-pending',
            'in-progress': 'status-in-progress',
            'completed': 'status-completed'
        };
        return classMap[status] || 'status-pending';
    }
}

// 全局函数
function viewTaskDetail(taskId) {
    // 保存当前任务ID到本地存储，供详情页使用
    dataStore.set('currentTaskId', taskId);
    location.href = `任务详情.html?taskId=${encodeURIComponent(taskId)}`;
}

function prevSlide() {
    if (window.mainCarousel) {
        window.mainCarousel.prev();
    }
}

function nextSlide() {
    if (window.mainCarousel) {
        window.mainCarousel.next();
    }
}

function goToSlide(index) {
    if (window.mainCarousel) {
        window.mainCarousel.goToSlide(index);
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 检查并初始化轮播图（如果存在）
    const carouselContainer = document.getElementById('carouselContainer');
    if (carouselContainer && carouselContainer.querySelectorAll('.carousel-item').length > 0) {
        try {
            window.mainCarousel = new Carousel('carouselContainer');
        } catch (error) {
            console.warn('轮播图初始化失败:', error);
        }
    }
    
    // 初始化热门任务（如果存在）
    const hotTaskList = document.getElementById('hotTaskList');
    if (hotTaskList) {
        try {
            window.hotTaskManager = new HotTaskManager('hotTaskList');
        } catch (error) {
            console.warn('热门任务初始化失败:', error);
        }
    }
    
    // 添加页面交互效果
    addPageInteractions();
    
    // 检查用户登录状态
    checkLoginStatus();
});

// 添加页面交互效果
function addPageInteractions() {
    // 快速入口悬停效果
    const accessItems = document.querySelectorAll('.access-item');
    accessItems.forEach(item => {
        item.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
        });
        
        item.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
    });
    
    // 任务卡片点击效果
    document.addEventListener('click', function(e) {
        if (e.target.closest('.task-card')) {
            const card = e.target.closest('.task-card');
            card.style.transform = 'scale(0.98)';
            setTimeout(() => {
                card.style.transform = '';
            }, 150);
        }
    });
}

// 检查登录状态
function checkLoginStatus() {
    const currentUser = stateManager.state.currentUser;
    if (currentUser) {
        // 用户已登录，更新界面
        updateUserInterface(currentUser);
    } else {
        // 用户未登录，显示登录提示
        showLoginPrompt();
    }
}

// 更新用户界面
function updateUserInterface(user) {
    const userInfoEl = document.querySelector('.user-info');
    if (userInfoEl) {
        userInfoEl.innerHTML = `
            <span class="user-name">${user.name}</span>
            <button class="login-btn" onclick="location.href='profile.html'">个人中心</button>
        `;
    }
    
    // 根据用户角色显示不同的快速入口
    updateQuickAccess(user.role);
}

// 更新快速入口
function updateQuickAccess(role) {
    const accessGrid = document.querySelector('.access-grid');
    if (!accessGrid) return;
    
    if (role === 'runner') {
        // 跑腿员视角
        accessGrid.innerHTML = `
            <div class="access-item" onclick="location.href='任务列表.html'">
                <div class="access-icon"><img src="images/icon-tasks.svg" class="icon-img"></div>
                <span class="access-text">任务列表</span>
            </div>
            <div class="access-item" onclick="location.href='任务状态.html'">
                <div class="access-icon"><img src="images/runner-avatar.svg" class="icon-img"></div>
                <span class="access-text">进行中</span>
            </div>
            <div class="access-item" onclick="location.href='个人中心.html'">
                <div class="access-icon"><img src="images/icon-profile.svg" class="icon-img"></div>
                <span class="access-text">个人中心</span>
            </div>
            <div class="access-item" onclick="location.href='消息.html'">
                <div class="access-icon"><img src="images/icon-messages.svg" class="icon-img"></div>
                <span class="access-text">消息中心</span>
            </div>
        `;
    }
}

// 显示登录提示
function showLoginPrompt() {
    // 可以在这里添加未登录时的特殊提示或功能限制
    console.log('用户未登录');
}

// 页面可见性变化处理
document.addEventListener('visibilitychange', function() {
    if (window.mainCarousel) {
        if (document.hidden) {
            window.mainCarousel.stopAutoPlay();
        } else {
            window.mainCarousel.startAutoPlay();
        }
    }
});

// 页面卸载前清理资源
window.addEventListener('beforeunload', function() {
    if (window.mainCarousel) {
        window.mainCarousel.stopAutoPlay();
    }
});

// 性能监控（开发环境）
if (window.performance) {
    window.addEventListener('load', function() {
        const perfData = window.performance.timing;
        const loadTime = perfData.loadEventEnd - perfData.navigationStart;
        console.log(`页面加载时间: ${loadTime}ms`);
        
        if (loadTime > 2000) {
            console.warn('页面加载时间超过2秒，建议优化');
        }
    });
}