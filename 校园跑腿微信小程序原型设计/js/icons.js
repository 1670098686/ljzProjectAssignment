// 图标库 - 统一管理所有图标，避免重复定义
const Icons = {
    // 导航图标
    HOME: 'images/icon-home.svg',
    TASKS: 'images/icon-tasks.svg',
    ADD: 'images/icon-publish.svg',
    MESSAGES: 'images/icon-messages.svg',
    PROFILE: 'images/icon-profile.svg',
    
    // 功能图标
    PUBLISH: 'images/icon-publish.svg',
    RUNNER: 'images/runner-avatar.svg',
    NOTIFICATION: 'images/icon-info.svg',
    
    // 表单图标
    PHONE: 'images/icon-other.svg', // 暂无专用，用other代替或新增
    LOCK: 'images/icon-other.svg',
    LOCATION: 'images/icon-location.svg',
    TARGET: 'images/icon-location.svg',
    CLOCK: 'images/icon-clock.svg',
    NOTE: 'images/icon-document.svg',
    MONEY: 'images/icon-money.svg',
    
    // 任务类型图标
    PACKAGE: 'images/icon-package.svg',
    FOOD: 'images/icon-food.svg',
    DOCUMENT: 'images/icon-document.svg',
    OTHER: 'images/icon-other.svg',
    
    // 状态图标
    SUCCESS: 'images/icon-success.svg',
    WARNING: 'images/icon-warning.svg',
    ERROR: 'images/icon-error.svg',
    INFO: 'images/icon-info.svg',
    URGENT: 'images/icon-urgent.svg',
    
    // 元数据图标
    DISTANCE: 'images/icon-location.svg',
    TIME: 'images/icon-clock.svg',
    
    // 消息图标
    SYSTEM: 'images/icon-info.svg',
    TASK: 'images/icon-tasks.svg',
    TRANSACTION: 'images/icon-money.svg'
};

function getIconHtml(iconPath, className = 'icon-img') {
    return `<img src="${iconPath}" class="${className}" alt="icon">`;
}

// 获取底部标签栏图标
function getTabbarIcon(page) {
    const icons = {
        'home': Icons.HOME,
        'tasks': Icons.TASKS,
        'publish': Icons.ADD,
        'messages': Icons.MESSAGES,
        'profile': Icons.PROFILE
    };
    return getIconHtml(icons[page] || Icons.INFO);
}

// 获取任务类型图标
function getTaskTypeIcon(type) {
    const icons = {
        '快递': Icons.PACKAGE,
        '餐食': Icons.FOOD,
        '文件': Icons.DOCUMENT,
        '其他': Icons.OTHER
    };
    return getIconHtml(icons[type] || Icons.OTHER);
}

// 获取消息类型图标
function getMessageTypeIcon(type) {
    const icons = {
        'system': Icons.SYSTEM,
        'task': Icons.TASK,
        'transaction': Icons.TRANSACTION
    };
    return getIconHtml(icons[type] || Icons.INFO);
}