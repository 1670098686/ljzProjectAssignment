// 图标管理器 - 统一管理所有页面的图标，避免重复定义
class IconManager {
    constructor() {
        this.icons = {
            // 导航图标
            'back-icon': '←',
            'tabbar-home-icon': '<img src="images/icon-home.svg" class="icon-img">',
            'tabbar-tasks-icon': '<img src="images/icon-tasks.svg" class="icon-img">',
            'tabbar-publish-icon': '<img src="images/icon-publish.svg" class="icon-img">',
            'tabbar-messages-icon': '<img src="images/icon-messages.svg" class="icon-img">',
            'tabbar-profile-icon': '<img src="images/icon-profile.svg" class="icon-img">',
            
            // 功能图标
            'messages-icon': '<img src="images/icon-messages.svg" class="icon-img">',
            'profile-icon': '<img src="images/icon-profile.svg" class="icon-img">',
            'avatar-icon': '<img src="images/icon-profile.svg" class="icon-img">',
            'publish-icon': '<img src="images/icon-publish.svg" class="icon-img">',
            'tasks-icon': '<img src="images/icon-tasks.svg" class="icon-img">',
            'runner-icon': '<img src="images/runner-avatar.svg" class="icon-img">',
            'notification-icon': '<img src="images/icon-info.svg" class="icon-img">',
            
            // 表单图标
            'form-icon': '<img src="images/icon-other.svg" class="icon-img">',
            'code-icon': '<img src="images/icon-other.svg" class="icon-img">',
            'tip-icon': '<img src="images/icon-success.svg" class="icon-img">',
            'section-title-icon': '<img src="images/icon-tasks.svg" class="icon-img">',
            'task-type-section-icon': '<img src="images/icon-tasks.svg" class="icon-img">',
            'task-details-section-icon': '📝',
            'budget-section-icon': '<img src="images/icon-money.svg" class="icon-img">',
            'pickup-location-icon': '<img src="images/icon-location.svg" class="icon-img">',
            'delivery-location-icon': '<img src="images/icon-location.svg" class="icon-img">',
            'time-requirement-icon': '<img src="images/icon-clock.svg" class="icon-img">',
            'task-notes-icon': '<img src="images/icon-tasks.svg" class="icon-img">',
            
            // 任务类型图标
            'package-icon': '<img src="images/icon-package.svg" class="icon-img">',
            'food-icon': '<img src="images/icon-food.svg" class="icon-img">',
            'document-icon': '<img src="images/icon-document.svg" class="icon-img">',
            'other-icon': '<img src="images/icon-other.svg" class="icon-img">',
            
            // 状态图标
            'urgent-icon': '<img src="images/icon-urgent.svg" class="icon-img">',
            'publish-icon-btn': '<img src="images/icon-publish.svg" class="icon-img">',
            'success-icon': '<img src="images/icon-success.svg" class="icon-img">',
            'info-icon': '<img src="images/icon-info.svg" class="icon-img">'
        };
    }
    
    // 设置页面图标
    setPageIcons() {
        Object.keys(this.icons).forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = this.icons[id];
            }
        });
    }
    
    // 设置通用类图标
    setClassIcons() {
        // 设置所有具有特定类的图标
        const iconSelectors = {
            '.form-icon': '<img src="images/icon-other.svg" class="icon-img">',
            '.tabbar-item-icon': '<img src="images/icon-home.svg" class="icon-img">', // 这个需要特殊处理
            '.grid-icon': '<img src="images/icon-publish.svg" class="icon-img">', // 这个需要特殊处理
            '.task-type-icon': '<img src="images/icon-package.svg" class="icon-img">' // 这个需要特殊处理
        };
        
        Object.keys(iconSelectors).forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                if (!element.id) {
                    element.textContent = iconSelectors[selector];
                }
            });
        });
    }
    
    // 初始化图标管理器
    init() {
        this.setPageIcons();
        this.setClassIcons();
    }
}

// 创建全局图标管理器实例
window.iconManager = new IconManager();

// 页面加载完成后初始化图标
document.addEventListener('DOMContentLoaded', function() {
    window.iconManager.init();
});