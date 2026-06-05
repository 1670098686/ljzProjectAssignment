// 关于页面JavaScript

// 页面加载完成后的初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeAboutPage();
    setupEventListeners();
    loadAppInfo();
});

// 初始化关于页面
function initializeAboutPage() {
    // 设置页面标题
    document.title = '关于我们 - 校园跑腿';
    
    // 检查用户登录状态
    checkUserStatus();
}

// 设置事件监听器
function setupEventListeners() {
    // FAQ展开/收起事件
    setupFAQToggle();
    
    // 联系信息点击事件
    setupContactActions();
    
    // 页面滚动事件
    setupScrollEffects();
}

// 设置FAQ展开/收起
function setupFAQToggle() {
    const faqItems = document.querySelectorAll('.faq-item');
    
    faqItems.forEach(item => {
        // 移除内联onclick，使用事件监听器
        item.addEventListener('click', function(e) {
            // 防止事件冒泡
            e.stopPropagation();
            toggleFAQ(this);
        });
    });
    
    // 默认展开第一个FAQ
    if (faqItems.length > 0) {
        setTimeout(() => {
            toggleFAQ(faqItems[0]);
        }, 500);
    }
}

// 切换FAQ显示状态
function toggleFAQ(faqItem) {
    const isActive = faqItem.classList.contains('active');
    const allFaqItems = document.querySelectorAll('.faq-item');
    
    // 关闭所有其他FAQ
    allFaqItems.forEach(item => {
        if (item !== faqItem) {
            item.classList.remove('active');
        }
    });
    
    // 切换当前FAQ状态
    if (!isActive) {
        faqItem.classList.add('active');
        
        // 添加展开动画
        const answer = faqItem.querySelector('.faq-answer');
        answer.style.display = 'block';
        answer.style.animation = 'fadeIn 0.3s ease';
    } else {
        faqItem.classList.remove('active');
        
        // 添加收起动画
        const answer = faqItem.querySelector('.faq-answer');
        answer.style.animation = 'fadeOut 0.3s ease';
        setTimeout(() => {
            answer.style.display = 'none';
        }, 300);
    }
}

// 设置联系信息点击事件
function setupContactActions() {
    const contactItems = document.querySelectorAll('.contact-item');
    
    contactItems.forEach(item => {
        item.addEventListener('click', function() {
            const label = this.querySelector('.contact-label').textContent;
            const value = this.querySelector('.contact-value').textContent;
            
            handleContactAction(label, value);
        });
    });
}

// 处理联系动作
function handleContactAction(label, value) {
    switch(label) {
        case '邮箱':
            copyToClipboard(value, '邮箱地址');
            break;
        case '客服电话':
            makePhoneCall(value);
            break;
        case '办公地址':
            showLocationInfo(value);
            break;
    }
}

// 复制到剪贴板
function copyToClipboard(text, type) {
    // 模拟复制功能（实际环境中需要使用Clipboard API）
    const tempInput = document.createElement('textarea');
    tempInput.value = text;
    document.body.appendChild(tempInput);
    tempInput.select();
    
    try {
        const successful = document.execCommand('copy');
        if (successful) {
            showToast(type + '已复制到剪贴板', 'success');
        } else {
            showToast('复制失败，请手动复制', 'error');
        }
    } catch (err) {
        showToast('复制失败：' + err, 'error');
    }
    
    document.body.removeChild(tempInput);
}

// 模拟拨打电话
function makePhoneCall(phoneNumber) {
    Dialog.show({
        title: '拨打电话',
        content: `是否拨打客服电话：<br><strong>${phoneNumber}</strong>`,
        buttons: [
            { text: '取消', type: 'secondary' },
            { text: '拨打', type: 'primary' }
        ]
    }).then(result => {
        if (result === '拨打') {
            // 模拟拨打电话
            showToast('正在呼叫客服...', 'info');
            
            // 记录客服联系记录
            const contactLog = {
                type: '客服电话',
                number: phoneNumber,
                time: new Date().toLocaleString(),
                userId: Storage.get('userInfo')?.userId || 'anonymous'
            };
            
            saveContactLog(contactLog);
            
            setTimeout(() => {
                showToast('电话已接通', 'success');
            }, 2000);
        }
    });
}

// 显示位置信息
function showLocationInfo(location) {
    Dialog.show({
        title: '办公地址',
        content: `
            <div style="text-align: center;">
                <div style="font-size: 16px; margin-bottom: 10px;">${location}</div>
                <div style="font-size: 12px; color: #666; margin-bottom: 15px;">
                    工作时间：周一至周五 9:00-18:00
                </div>
                <div style="background: #f5f5f5; padding: 10px; border-radius: 5px; font-size: 12px;">
                    <img src="images/icon-info.svg" class="icon-img"> 温馨提示：来访前请提前预约
                </div>
            </div>
        `,
        buttons: [
            { text: '知道了', type: 'primary' }
        ]
    });
}

// 设置滚动效果
function setupScrollEffects() {
    let lastScrollTop = 0;
    const header = document.querySelector('.about-header');
    
    window.addEventListener('scroll', function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        
        if (scrollTop > lastScrollTop && scrollTop > 100) {
            // 向下滚动，隐藏头部
            header.style.transform = 'translateY(-100%)';
        } else {
            // 向上滚动，显示头部
            header.style.transform = 'translateY(0)';
        }
        
        lastScrollTop = scrollTop;
    });
}

// 加载应用信息
function loadAppInfo() {
    // 模拟应用信息数据
    const appInfo = {
        name: '校园跑腿',
        version: '1.0.0',
        description: '校园专属即时服务平台',
        lastUpdate: '2023-12-15',
        features: [
            '快递取件', '餐食配送', '文件传递', '安全保障'
        ]
    };
    
    // 更新页面显示
    updateAppDisplay(appInfo);
    
    // 记录页面访问
    recordPageVisit();
}

// 更新应用显示
function updateAppDisplay(appInfo) {
    document.querySelector('.app-name').textContent = appInfo.name;
    document.querySelector('.app-version').textContent = '版本 ' + appInfo.version;
    document.querySelector('.app-description').textContent = appInfo.description;
    document.querySelector('.version-info').textContent = 
        `当前版本：${appInfo.version} | 最后更新：${appInfo.lastUpdate}`;
}

// 检查用户状态
function checkUserStatus() {
    const userInfo = Storage.get('userInfo');
    
    if (userInfo && userInfo.userId) {
        // 已登录用户
        console.log('用户已登录:', userInfo.userId);
    } else {
        // 未登录用户
        console.log('用户未登录');
    }
}

// 保存联系记录
function saveContactLog(contactLog) {
    const contactLogs = Storage.get('contactLogs') || [];
    contactLogs.push(contactLog);
    Storage.set('contactLogs', contactLogs);
}

// 记录页面访问
function recordPageVisit() {
    const visitLog = {
        page: 'about',
        timestamp: new Date().toISOString(),
        userId: Storage.get('userInfo')?.userId || 'anonymous'
    };
    
    const pageVisits = Storage.get('pageVisits') || [];
    pageVisits.push(visitLog);
    Storage.set('pageVisits', pageVisits);
}

// 返回上一页
function goBack() {
    window.history.back();
}

// 页面可见性变化处理
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        // 页面重新可见时刷新数据
        loadAppInfo();
    }
});

// 错误处理
window.addEventListener('error', function(e) {
    console.error('关于页面错误:', e.error);
    showToast('页面加载出错，请刷新重试', 'error');
});

// 添加CSS动画关键帧
const style = document.createElement('style');
style.textContent = `
    @keyframes fadeOut {
        from { opacity: 1; transform: translateY(0); }
        to { opacity: 0; transform: translateY(-10px); }
    }
    
    .about-header {
        transition: transform 0.3s ease;
        position: sticky;
        top: 0;
        z-index: 100;
    }
`;
document.head.appendChild(style);

// 导出函数供其他页面使用
window.about = {
    toggleFAQ,
    handleContactAction,
    goBack
};