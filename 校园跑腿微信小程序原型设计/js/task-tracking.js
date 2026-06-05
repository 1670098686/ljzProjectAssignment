// 任务追踪页面JavaScript

let __currentTaskId = null;
let __currentTaskData = null;

// 页面加载完成后的初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeTaskTracking();
    startRealTimeUpdates();
    bindStageClicks();
});

// 初始化任务追踪功能
function initializeTaskTracking() {
    // 从URL参数获取任务ID
    const urlParams = new URLSearchParams(window.location.search);
    const taskId = urlParams.get('taskId') || 'T20241215001';
    __currentTaskId = taskId;
    
    // 更新页面标题和任务ID
    document.querySelector('.task-id').textContent = '#' + taskId;
    
    // 加载任务数据
    loadTaskData(taskId);
    
    // 初始化地图模拟
    initializeMapSimulation();
}

// 加载任务数据
function loadTaskData(taskId) {
    const found = (window.TaskFlow && typeof TaskFlow.findTask === 'function')
        ? TaskFlow.findTask(taskId)
        : (() => {
            const tasks = (window.dataStore && dataStore.get('tasks')) || [];
            return Array.isArray(tasks) ? tasks.find(t => String(t.id) === String(taskId) || String(t.taskId || '') === String(taskId)) : null;
        })();

    const priceVal = (val) => {
        if (typeof val === 'number') return val.toFixed(2);
        const num = parseFloat(val);
        return isNaN(num) ? '0.00' : num.toFixed(2);
    };

    const deriveStatus = (task) => {
        const stage = task.progressStage || task.status;
        if (task.status === 'completed' || stage === 'completed') return { step: 'completed', progress: 100 };
        if (task.status === 'pending') return { step: 'pending', progress: 10 };
        if (stage === 'accepted') return { step: 'accepted', progress: 25 };
        if (stage === 'picking_up') return { step: 'picking_up', progress: 50 };
        if (stage === 'delivering') return { step: 'delivering', progress: 80 };
        return { step: 'pending', progress: 20 };
    };

    const toTaskData = (task) => {
        const derived = deriveStatus(task);
        return {
            taskId: taskId,
            type: task.type || '任务',
            pickupLocation: task.from || task.pickupLocation || '取件地点未填写',
            deliveryLocation: task.to || task.deliveryLocation || '送达地点未填写',
            runnerName: task.runnerName || task.runner || '待分配',
            runnerRating: 4.6,
            completedTasks: 32,
            onTimeRate: '98%',
            creditScore: 'A+',
            status: derived.step,
            progress: derived.progress,
            eta: task.eta || '15分钟',
            price: priceVal(task.price)
        };
    };

    // 若有真实任务则使用，否则使用空数据
    const taskData = found ? toTaskData(found) : {
        taskId: taskId,
        type: '',
        pickupLocation: '',
        deliveryLocation: '',
        runnerName: '',
        runnerRating: 0,
        completedTasks: 0,
        onTimeRate: '0%',
        creditScore: '',
        status: '',
        progress: 0,
        eta: '',
        price: '0.00'
    };

    __currentTaskData = taskData;
    updateTaskDisplay(taskData);
    if (window.dataStore) {
        dataStore.set('currentTask', taskData);
    }
}

// 更新任务显示
function updateTaskDisplay(taskData) {
    // 更新任务详情
    document.querySelector('.detail-item:nth-child(1) .detail-value').textContent = taskData.type;
    document.querySelector('.detail-item:nth-child(2) .detail-value').textContent = taskData.pickupLocation;
    document.querySelector('.detail-item:nth-child(3) .detail-value').textContent = taskData.deliveryLocation;
    document.querySelector('.detail-item:nth-child(4) .detail-value').textContent = taskData.eta;
    document.querySelector('.detail-item:nth-child(5) .detail-value').textContent = '¥' + taskData.price;
    
    // 更新跑腿员信息
    document.querySelector('.runner-name').textContent = taskData.runnerName;
    document.querySelector('.rating-score').textContent = taskData.runnerRating;
    document.querySelector('.stat-item:nth-child(1) .stat-value').textContent = taskData.completedTasks;
    document.querySelector('.stat-item:nth-child(2) .stat-value').textContent = taskData.onTimeRate;
    document.querySelector('.stat-item:nth-child(3) .stat-value').textContent = taskData.creditScore;
    
    // 更新进度条
    updateProgressBar(taskData.progress);
    
    // 更新状态步骤
    updateStatusSteps(taskData.status);
    updateStatusLabel(taskData.status);
}

// 更新进度条
function updateProgressBar(progress) {
    const progressFill = document.getElementById('progressFill');
    progressFill.style.width = progress + '%';
}

// 更新状态步骤
function updateStatusSteps(status) {
    const steps = document.querySelectorAll('.status-step');
    
    // 重置所有步骤
    steps.forEach(step => {
        step.classList.remove('active');
    });
    
    // 根据状态激活相应步骤
    switch(status) {
        case 'pending':
            steps[0].classList.add('active');
            break;
        case 'accepted':
            steps[0].classList.add('active');
            break;
        case 'picking_up':
            steps[0].classList.add('active');
            steps[1].classList.add('active');
            break;
        case 'delivering':
            steps[0].classList.add('active');
            steps[1].classList.add('active');
            steps[2].classList.add('active');
            break;
        case 'completed':
            steps.forEach(step => step.classList.add('active'));
            break;
    }
}

function updateStatusLabel(status) {
    const labelEl = document.querySelector('.card .task-id')?.nextElementSibling;
    if (!labelEl) return;
    const map = {
        pending: '待接单',
        accepted: '已接单',
        picking_up: '取件中',
        delivering: '配送中',
        delivered: '待确认',
        completed: '已完成'
    };
    labelEl.textContent = map[status] || '进行中';
}

function bindStageClicks() {
    const steps = Array.from(document.querySelectorAll('.status-step'));
    const stageOrder = ['accepted', 'picking_up', 'delivering', 'completed'];
    steps.forEach((step, idx) => {
        const stage = stageOrder[idx] || 'accepted';
        step.dataset.stage = stage;
        step.style.cursor = 'pointer';
        step.addEventListener('click', () => applyStage(stage));
    });
}

function applyStage(stage) {
    if (!__currentTaskId) return;

    const progressMap = {
        accepted: 25,
        picking_up: 50,
        delivering: 80,
        completed: 100
    };

    // 更新存储中的任务状态
    if (window.TaskFlow && typeof TaskFlow.updateTask === 'function') {
        TaskFlow.updateTask(__currentTaskId, task => {
            if (!task) return task;
            const next = { ...task, progressStage: stage };
            if (stage === 'completed') {
                next.status = 'completed';
                next.runnerConfirmed = true;
                next.publisherConfirmed = true;
                next.completeTime = Date.now();
            } else {
                next.status = 'in-progress';
            }
            next.timeline = [...(next.timeline || []), { type: stage, time: Date.now() }];
            return next;
        });
    }

    // 更新页面展示
    const status = stage === 'completed' ? 'completed' : stage;
    updateStatusSteps(status);
    updateProgressBar(progressMap[stage] || 20);
    updateStatusLabel(status);
    if (status === 'completed') {
        setTimeout(() => {
            window.location.href = `评价.html?taskId=${encodeURIComponent(__currentTaskId)}`;
        }, 400);
    }
    updateRunnerPosition(progressMap[stage] || 20);

    // 更新当前任务数据缓存
    if (__currentTaskData) {
        __currentTaskData.status = status;
        __currentTaskData.progress = progressMap[stage] || __currentTaskData.progress;
    }
}

// 初始化地图模拟
function initializeMapSimulation() {
    // 初始化跑腿员位置到当前进度
    simulateRunnerMovement();
    
    // 模拟ETA更新
    simulateEtaUpdate();
}

// 根据进度设置跑腿员位置（可被点击阶段直接驱动）
function simulateRunnerMovement() {
    const progress = (__currentTaskData && __currentTaskData.progress) || 25;
    updateRunnerPosition(progress);
}

function updateRunnerPosition(progress) {
    const runnerPoint = document.getElementById('runnerPoint');
    if (!runnerPoint) return;
    // 40%进度对应起点，100%对应终点附近
    const position = 40 + Math.max(0, Math.min(100, progress)) * 0.6; // progress 0-100 -> 40%-100%
    runnerPoint.style.left = position + '%';
}

// 模拟ETA更新
function simulateEtaUpdate() {
    const etaElement = document.getElementById('etaTime');
    let etaMinutes = 15;
    
    setInterval(() => {
        // 随机减少ETA（模拟接近目的地）
        if (etaMinutes > 0) {
            etaMinutes -= Math.random();
            etaMinutes = Math.max(0, etaMinutes);
            
            const displayMinutes = Math.ceil(etaMinutes);
            etaElement.textContent = displayMinutes > 0 ? displayMinutes + '分钟' : '即将到达';
        }
    }, 60000); // 每分钟更新一次
}

// 开始实时更新
function startRealTimeUpdates() {
    // 显示实时更新提示
    const updateNotice = document.getElementById('updateNotice');
    updateNotice.style.display = 'flex';
    
    // 模拟实时位置更新
    setInterval(() => {
        // 这里可以添加实际的实时位置更新逻辑
        console.log('位置已更新');
    }, 10000); // 每10秒更新一次
}

// 联系跑腿员
function contactRunner() {
    modalManager.showConfirm(
        '联系跑腿员',
        '是否拨打跑腿员电话？<br><small>电话：138****1234</small>',
        '拨打',
        '取消'
    ).then(result => {
        if (result) {
            // 模拟拨打电话
            CommonUtils.showToast('正在呼叫跑腿员...', 'info');
            
            setTimeout(() => {
                CommonUtils.showToast('电话已接通', 'success');
            }, 2000);
        }
    });
}

// 发送消息
function sendMessage() {
    ensureModalStyles();
    // 创建弹窗元素
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3 class="modal-title">发送消息</h3>
                <button class="modal-close" onclick="this.closest('.modal').remove()">×</button>
            </div>
            <div class="modal-body">
                <textarea id="messageInput" placeholder="请输入要发送的消息..." style="width: 100%; height: 100px; padding: 10px; border: 1px solid #ddd; border-radius: 4px; resize: none;"></textarea>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" onclick="this.closest('.modal').remove()">取消</button>
                <button class="btn btn-primary" onclick="sendMessageConfirm()">发送</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    setTimeout(() => modal.classList.add('show'), 10);
}

// 确保弹窗样式存在，防止在未引入样式文件时弹窗错位
function ensureModalStyles() {
    if (document.getElementById('taskTrackingModalStyles')) return;
    const style = document.createElement('style');
    style.id = 'taskTrackingModalStyles';
    style.textContent = `
        .modal {
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.45);
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            transition: opacity 0.2s ease;
            z-index: 9999;
        }
        .modal.show { opacity: 1; }
        .modal-content {
            background: #fff;
            width: calc(100% - 48px);
            max-width: 420px;
            border-radius: 12px;
            box-shadow: 0 12px 32px rgba(0,0,0,0.18);
            overflow: hidden;
        }
        .modal-header, .modal-footer { padding: 12px 16px; }
        .modal-body { padding: 0 16px 16px; }
        .modal-header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #f0f0f0; }
        .modal-title { margin: 0; font-size: 16px; font-weight: 600; }
        .modal-close { background: none; border: none; font-size: 20px; cursor: pointer; line-height: 1; }
        .modal-footer { display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid #f0f0f0; }
    `;
    document.head.appendChild(style);
}

// 发送消息确认
function sendMessageConfirm() {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput ? messageInput.value.trim() : '';
    
    // 移除弹窗
    const modal = document.querySelector('.modal');
    if (modal) {
        modal.remove();
    }
    
    if (!message) {
        CommonUtils.showToast('请输入消息内容', 'error');
        return;
    }

    // 模拟发送消息
    CommonUtils.showToast('消息发送成功', 'success');

    // 保存消息记录（对 dataStore/MessageStore 做兜底，避免未初始化时报错）
    const fallbackTime = new Date().toLocaleTimeString();
    let taskId = '';
    try {
        const currentTask = window.dataStore ? dataStore.get('currentTask') : null;
        taskId = currentTask ? currentTask.taskId : '';
    } catch (e) {
        console.warn('读取当前任务失败', e);
    }

    const messageRecord = { type: 'sent', content: message, time: fallbackTime, taskId };

    try {
        if (window.dataStore) {
            const messages = dataStore.get('taskMessages') || [];
            messages.push(messageRecord);
            dataStore.set('taskMessages', messages);
        } else {
            const raw = localStorage.getItem('taskMessages');
            const messages = raw ? JSON.parse(raw) : [];
            messages.push(messageRecord);
            localStorage.setItem('taskMessages', JSON.stringify(messages));
        }
    } catch (e) {
        console.warn('存储任务消息失败', e);
    }

    try {
        const threadId = taskId ? `chat_${taskId}` : 'chat';
        if (window.MessageStore) {
            MessageStore.addMessage(threadId, message, 'self', { title: taskId ? `任务 ${taskId}` : '跑腿员' });
            MessageStore.markRead(threadId);
        } else {
            CommonUtils.showToast('消息中心未初始化，仅本地保存', 'warning');
        }
    } catch (e) {
        console.warn('写入消息中心失败', e);
        CommonUtils.showToast('消息未同步到消息中心', 'error');
    }
}

// 返回上一页
function goBack() {
    window.history.back();
}

// 页面可见性变化处理
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        // 页面重新可见时刷新数据
        const currentTask = dataStore.get('currentTask');
        if (currentTask) {
            loadTaskData(currentTask.taskId);
        }
    }
});

// 错误处理
window.addEventListener('error', function(e) {
    console.error('任务追踪页面错误:', e.error);
    if (window.CommonUtils && CommonUtils.showToast) {
        CommonUtils.showToast('页面加载出错，请刷新重试', 'error');
    }
});

// 导出函数供其他页面使用
window.taskTracking = {
    loadTaskData,
    updateTaskDisplay,
    contactRunner,
    sendMessage
};