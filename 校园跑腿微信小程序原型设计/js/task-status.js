// 任务状态页面JavaScript

// 页面全局变量
let currentTasks = [];
let filteredTasks = [];
let currentFilters = {
    taskType: ['快递', '餐食', '文件', '其他'],
    taskStatus: ['进行中', '待接单', '已完成', '已取消']
};

function showToast(message, type = 'info') {
    if (window.CommonUtils && CommonUtils.showToast) {
        CommonUtils.showToast(message, type);
        return;
    }
    alert(message);
}

// 页面加载完成后的初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeTaskStatus();
    setupEventListeners();
    loadTasks();
});

// 初始化任务状态页面
function initializeTaskStatus() {
    // 设置页面标题
    document.title = '任务状态 - 校园跑腿';
    
    // 检查用户登录状态
    checkUserStatus();
    
    // 从URL参数获取任务ID
    const urlParams = new URLSearchParams(window.location.search);
    const taskId = urlParams.get('taskId');
    
    if (taskId) {
        // 如果有特定任务ID，设置筛选以显示该任务
        currentFilters.taskType = [];
        currentFilters.taskStatus = [];
        currentFilters.specificTaskId = taskId;
    }
    
    // 初始化筛选面板状态
    initializeFilterPanel();
}

// 设置事件监听器
function setupEventListeners() {
    // 筛选选项变化事件
    setupFilterChangeListeners();
    
    // 页面滚动事件
    setupScrollEvents();
    
    // 下拉刷新事件
    setupPullToRefresh();
}

// 初始化筛选面板
function initializeFilterPanel() {
    // 从本地存储加载筛选设置
    const savedFilters = Storage.get('taskFilters');
    if (savedFilters) {
        currentFilters = savedFilters;
        updateFilterUI();
    }
}

// 设置筛选选项变化监听
function setupFilterChangeListeners() {
    const filterInputs = document.querySelectorAll('.filter-option input');
    
    filterInputs.forEach(input => {
        input.addEventListener('change', function() {
            updateCurrentFilters();
        });
    });
}

// 更新当前筛选设置
function updateCurrentFilters() {
    // 更新任务类型筛选
    const typeInputs = document.querySelectorAll('input[name="taskType"]:checked');
    currentFilters.taskType = Array.from(typeInputs).map(input => input.value);
    
    // 更新任务状态筛选
    const statusInputs = document.querySelectorAll('input[name="taskStatus"]:checked');
    currentFilters.taskStatus = Array.from(statusInputs).map(input => input.value);
    
    // 保存筛选设置
    Storage.set('taskFilters', currentFilters);
    
    // 应用筛选
    applyFilter();
}

// 更新筛选UI
function updateFilterUI() {
    // 更新任务类型复选框
    const typeInputs = document.querySelectorAll('input[name="taskType"]');
    typeInputs.forEach(input => {
        input.checked = currentFilters.taskType.includes(input.value);
    });
    
    // 更新任务状态复选框
    const statusInputs = document.querySelectorAll('input[name="taskStatus"]');
    statusInputs.forEach(input => {
        input.checked = currentFilters.taskStatus.includes(input.value);
    });
}

// 加载任务数据
function loadTasks() {
    showLoadingState(true);

    // 模拟异步加载
    setTimeout(() => {
        const auth = CommonUtils.getAuth() || {};
        let sourceTasks = [];
        if (window.TaskFlow) {
            sourceTasks = TaskFlow.getTasksForUser(auth) || [];
        } else {
            const stored = (window.dataStore && dataStore.get('tasks')) || [];
            const cleared = localStorage.getItem('tasksCleared') === 'true';
            const fallback = (window.dataStore && dataStore.getPersistentTasks) ? dataStore.getPersistentTasks() : generateMockTasks();
            sourceTasks = Array.isArray(stored) && stored.length > 0 ? stored : (cleared ? [] : fallback);
        }

        const userId = auth.id || auth.openid;
        const mapped = (sourceTasks || []).map(task => mapTaskToDisplay(task, userId));

        currentTasks = mapped;
        filteredTasks = [...mapped];

        updateTaskDisplay();
        updateStats();

        showLoadingState(false);
    }, 300);
}

// 标准化任务数据供展示和筛选使用
function mapTaskToDisplay(task, userId) {
    const stage = task.progressStage || task.status;
    let displayStatus = '待接单';
    let filterStatus = '待接单';

    if (task.status === 'cancelled' || task.status === 'canceled') {
        displayStatus = '已取消';
        filterStatus = '已取消';
    } else if (task.status === 'completed' || stage === 'completed') {
        displayStatus = '已完成';
        filterStatus = '已完成';
    } else if (task.status === 'pending') {
        displayStatus = '待接单';
        filterStatus = '待接单';
    } else {
        filterStatus = '进行中';
        if (stage === 'delivered') {
            displayStatus = '待确认';
        } else if (stage === 'delivering') {
            displayStatus = '配送中';
        } else {
            displayStatus = '进行中';
        }
    }

    const id = task.id || task.taskId || (window.CommonUtils ? CommonUtils.generateId() : `task_${Date.now()}`);
    const priceVal = typeof task.price === 'number' ? task.price.toFixed(2) : (task.price || '0.00');
    const created = task.createTime ? new Date(task.createTime).toLocaleString() : new Date().toLocaleString();
    const locationText = task.location || (task.from && task.to ? `${task.from} → ${task.to}` : '');

    const isRunner = !!(userId && task.runnerId === userId);
    const isPublisher = !!(userId && task.publisherId === userId);

    return {
        ...task,
        id,
        status: displayStatus,
        filterStatus,
        progressStage: stage,
        price: priceVal,
        createTime: created,
        location: locationText,
        title: task.title || `${task.type || '任务'}任务`,
        isRunner,
        isPublisher
    };
}

// 清空模拟任务数据
function generateMockTasks() {
    return [];
}

// 更新任务显示
function updateTaskDisplay() {
    const taskList = document.getElementById('taskList');
    const emptyState = document.getElementById('emptyState');
    
    // 清空现有内容
    taskList.innerHTML = '';
    
    if (filteredTasks.length === 0) {
        // 显示空状态
        emptyState.classList.add('show');
        return;
    } else {
        emptyState.classList.remove('show');
    }
    
    // 按创建时间排序（最新的在前）
    const sortedTasks = [...filteredTasks].sort((a, b) => 
        new Date(b.createTime) - new Date(a.createTime)
    );
    
    // 生成任务卡片
    sortedTasks.forEach(task => {
        const taskCard = createTaskCard(task);
        taskList.appendChild(taskCard);
    });
}

// 创建任务卡片
function createTaskCard(task) {
    const card = document.createElement('div');
    card.className = 'task-card';
    card.onclick = () => viewTaskDetail(task);
    
    // 获取任务图标
    const icon = getTaskIcon(task.type);
    
    // 获取状态样式类
    const statusClass = getStatusClass(task.status);
    
    card.innerHTML = `
        <div class="task-card-header">
            <div class="task-info">
                <div class="task-type">
                    <div class="task-icon">${icon}</div>
                    <div class="task-title">${task.title}</div>
                </div>
                <div class="task-id">${task.id}</div>
                <div class="task-time">${task.createTime}</div>
            </div>
            <div class="task-status ${statusClass}">${task.status}</div>
        </div>
        
        <div class="task-details">
            <div class="task-location">${task.location}</div>
            <div class="task-price">¥${task.price}</div>
        </div>
        
        <div class="task-actions">
            ${getActionButtons(task)}
        </div>
    `;
    
    return card;
}

// 获取任务图标
function getTaskIcon(type) {
    const icons = {
        '快递': '<img src="images/icon-package.svg" class="icon-img">',
        '餐食': '<img src="images/icon-food.svg" class="icon-img">',
        '文件': '<img src="images/icon-document.svg" class="icon-img">',
        '其他': '<img src="images/icon-tasks.svg" class="icon-img">'
    };
    return icons[type] || '<img src="images/icon-tasks.svg" class="icon-img">';
}

// 获取状态样式类
function getStatusClass(status) {
    const classes = {
        '待接单': 'status-pending',
        '进行中': 'status-active',
        '配送中': 'status-active',
        '待确认': 'status-active',
        '已完成': 'status-completed',
        '已取消': 'status-cancelled'
    };
    return classes[status] || 'status-pending';
}

// 获取操作按钮
function getActionButtons(task) {
    const stage = task.progressStage || task.status;

    if (task.status === '待接单') {
        return task.isPublisher
            ? `<button class="btn btn-secondary" onclick="event.stopPropagation(); cancelTask('${task.id}')">取消</button>
               <button class="btn btn-primary" onclick="event.stopPropagation(); editTask('${task.id}')">编辑</button>`
                        : `<button class="btn btn-primary" onclick="event.stopPropagation(); viewTaskDetail({ id: '${task.id}' })">查看</button>`;
    }

    if (task.filterStatus === '进行中') {
        if (task.isRunner) {
            const deliverBtn = stage === 'delivered'
                ? `<button class="btn btn-secondary" onclick="event.stopPropagation();">等待确认</button>`
                : `<button class="btn btn-primary" onclick="event.stopPropagation(); runnerMarkDeliveredFromStatus('${task.id}')">标记送达</button>`;

            return `
                <button class="btn btn-primary" onclick="event.stopPropagation(); trackTask('${task.id}')">追踪</button>
                ${deliverBtn}
            `;
        }

        if (task.isPublisher) {
            if (task.status === '待确认') {
                return `<button class="btn btn-primary" onclick="event.stopPropagation(); confirmTaskCompletionFromStatus('${task.id}')">确认完成</button>`;
            }
            return `<button class="btn btn-primary" onclick="event.stopPropagation(); trackTask('${task.id}')">追踪</button>`;
        }

        return `<button class="btn btn-primary" onclick="event.stopPropagation(); trackTask('${task.id}')">追踪</button>`;
    }

    if (task.status === '已完成') {
        return `
            <button class="btn btn-primary" onclick="event.stopPropagation(); evaluateTask('${task.id}')">评价</button>
            <button class="btn btn-secondary" onclick="event.stopPropagation(); viewDetails('${task.id}')">详情</button>
        `;
    }

    if (task.status === '已取消') {
        return `
            <button class="btn btn-secondary" onclick="event.stopPropagation(); viewDetails('${task.id}')">详情</button>
            <button class="btn btn-primary" onclick="event.stopPropagation(); republishTask('${task.id}')">重新发布</button>
        `;
    }

    return '<button class="btn btn-secondary" onclick="event.stopPropagation(); viewDetails(\'${task.id}\')">详情</button>';
}

// 更新统计信息
function updateStats() {
    const totalTasks = filteredTasks.length;
    const activeTasks = filteredTasks.filter(task => task.filterStatus === '进行中').length;
    const completedTasks = filteredTasks.filter(task => task.filterStatus === '已完成').length;
    const pendingTasks = filteredTasks.filter(task => task.filterStatus === '待接单').length;
    
    document.getElementById('totalTasks').textContent = totalTasks;
    document.getElementById('activeTasks').textContent = activeTasks;
    document.getElementById('completedTasks').textContent = completedTasks;
    document.getElementById('pendingTasks').textContent = pendingTasks;
}

// 切换筛选面板显示
function toggleFilter() {
    const filterPanel = document.getElementById('filterPanel');
    filterPanel.classList.toggle('show');
}

// 应用筛选
function applyFilter() {
    filteredTasks = currentTasks.filter(task => {
        // 如果指定了特定任务ID，则只显示该任务
        if (currentFilters.specificTaskId) {
            return task.id === currentFilters.specificTaskId;
        }
        
        // 常规筛选逻辑
        return currentFilters.taskType.includes(task.type) &&
             currentFilters.taskStatus.includes(task.filterStatus || task.status);
    });
    
    updateTaskDisplay();
    updateStats();
    
    // 关闭筛选面板
    document.getElementById('filterPanel').classList.remove('show');
}

// 重置筛选
function resetFilter() {
    currentFilters = {
        taskType: ['快递', '餐食', '文件', '其他'],
        taskStatus: ['进行中', '待接单', '已完成', '已取消'],
        specificTaskId: null
    };
    
    updateFilterUI();
    applyFilter();
}

// 显示/隐藏加载状态
function showLoadingState(show) {
    const loadingState = document.getElementById('loadingState');
    const taskList = document.getElementById('taskList');
    
    if (show) {
        loadingState.classList.add('show');
        taskList.style.display = 'none';
    } else {
        loadingState.classList.remove('show');
        taskList.style.display = 'block';
    }
}

// 任务操作函数
function viewTaskDetail(task) {
    if (window.dataStore) {
        dataStore.set('currentTaskId', task.id);
    }

    // 根据任务状态跳转到对应页面
    if (task.status === '进行中') {
        window.location.href = `任务追踪.html?taskId=${encodeURIComponent(task.id)}`;
        return;
    }

    // 其他状态直接进入任务详情
    window.location.href = `任务详情.html?taskId=${encodeURIComponent(task.id)}`;
}

function showTaskDetailModal(task) {
    Dialog.show({
        title: '任务详情',
        content: `
            <div style="line-height: 1.6;">
                <div><strong>任务ID：</strong>${task.id}</div>
                <div><strong>任务类型：</strong>${task.type}</div>
                <div><strong>任务状态：</strong>${task.status}</div>
                <div><strong>地点：</strong>${task.location}</div>
                <div><strong>报酬：</strong>¥${task.price}</div>
                <div><strong>创建时间：</strong>${task.createTime}</div>
                ${task.runner ? `<div><strong>跑腿员：</strong>${task.runner}</div>` : ''}
            </div>
        `,
        buttons: [
            { text: '关闭', type: 'secondary' }
        ]
    });
}

function cancelTask(taskId) {
    Dialog.show({
        title: '确认取消',
        content: '确定要取消这个任务吗？',
        buttons: [
            { text: '再想想', type: 'secondary' },
            { text: '确定取消', type: 'primary' }
        ]
    }).then(result => {
        if (result === '确定取消') {
            // 模拟取消任务
            const taskIndex = currentTasks.findIndex(task => task.id === taskId);
            if (taskIndex !== -1) {
                currentTasks[taskIndex].status = '已取消';
                applyFilter();
                showToast('任务已取消', 'success');
            }
        }
    });
}

function editTask(taskId) {
    showToast('编辑功能开发中', 'info');
}

function trackTask(taskId) {
    window.location.href = `任务追踪.html?taskId=${encodeURIComponent(taskId)}`;
}

function contactRunner(taskId) {
    showToast('联系功能开发中', 'info');
}

function runnerMarkDeliveredFromStatus(taskId) {
    if (!window.TaskFlow) return;
    TaskFlow.markDelivered(taskId);
    showToast('已标记送达，等待用户确认', 'info');
    loadTasks();
}

function confirmTaskCompletionFromStatus(taskId) {
    if (!window.TaskFlow) return;
    TaskFlow.confirmCompletion(taskId);
    showToast('已确认完成，前往评价', 'success');
    setTimeout(() => {
        window.location.href = `评价.html?taskId=${encodeURIComponent(taskId)}`;
    }, 400);
    loadTasks();
}

function evaluateTask(taskId) {
    window.location.href = `任务详情.html?taskId=${encodeURIComponent(taskId)}`;
}

function viewDetails(taskId) {
    const task = currentTasks.find(t => t.id === taskId);
    if (task) {
        showTaskDetailModal(task);
    }
}

function republishTask(taskId) {
    showToast('重新发布功能开发中', 'info');
}

// 设置滚动事件
function setupScrollEvents() {
    let lastScrollTop = 0;
    
    window.addEventListener('scroll', function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const header = document.querySelector('.task-status-header');
        
        if (scrollTop > lastScrollTop && scrollTop > 100) {
            header.style.transform = 'translateY(-100%)';
        } else {
            header.style.transform = 'translateY(0)';
        }
        
        lastScrollTop = scrollTop;
    });
}

// 设置下拉刷新
function setupPullToRefresh() {
    let startY = 0;
    
    document.addEventListener('touchstart', function(e) {
        startY = e.touches[0].pageY;
    });
    
    document.addEventListener('touchmove', function(e) {
        const currentY = e.touches[0].pageY;
        const diff = currentY - startY;
        
        if (diff > 50 && window.scrollY === 0) {
            // 下拉刷新
            loadTasks();
        }
    });
}

// 检查用户状态
function checkUserStatus() {
    const auth = CommonUtils.getAuth();
    if (!auth || !auth.loggedIn) {
        showToast('请先登录', 'error');
        setTimeout(() => {
            window.location.href = '登录.html';
        }, 1500);
    }
}

// 跳转到发布任务页面
function goToPublish() {
    window.location.href = '任务发布.html';
}

// 返回上一页
function goBack() {
    window.history.back();
}

// 页面可见性变化处理
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        // 页面重新可见时刷新数据
        loadTasks();
    }
});

// 错误处理
window.addEventListener('error', function(e) {
    console.error('任务状态页面错误:', e.error);
    showToast('页面加载出错，请刷新重试', 'error');
});

// 导出函数供其他页面使用
window.taskStatus = {
    loadTasks,
    applyFilter,
    resetFilter,
    goBack
};