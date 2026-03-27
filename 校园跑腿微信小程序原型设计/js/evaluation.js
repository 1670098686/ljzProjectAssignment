// 评价页面JavaScript

// 页面全局变量
let currentRating = 0;
let selectedTags = [];
let commentText = '';
let isAnonymous = false;
let currentTaskDetail = null;
let existingEvaluation = null;

function getLocal(key, def = null) {
    try {
        const val = localStorage.getItem(key);
        return val ? JSON.parse(val) : def;
    } catch (e) {
        return def;
    }
}

function setLocal(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
        console.warn('存储失败', e);
    }
}

function toast(msg, type = 'info') {
    if (window.CommonUtils && CommonUtils.showToast) {
        CommonUtils.showToast(msg, type);
    } else {
        alert(msg);
    }
}

// 页面加载完成后的初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeEvaluation();
    setupEventListeners();
    loadTaskData();
    hydrateExistingEvaluation();
});

// 初始化评价功能
function initializeEvaluation() {
    const taskId = resolveTaskId();
    if (!taskId) {
        toast('未找到任务，无法评价', 'error');
        const submitBtn = document.getElementById('submitBtn');
        if (submitBtn) submitBtn.disabled = true;
        return;
    }
    setLocal('currentEvaluationTaskId', taskId);
}

function resolveTaskId() {
    const urlParams = new URLSearchParams(window.location.search);
    const fromUrl = urlParams.get('taskId');
    if (fromUrl) return fromUrl;

    // 回退到全局存储的当前任务
    try {
        const stored = (window.dataStore && (dataStore.get('currentTaskId') || (dataStore.get('currentTask') && dataStore.get('currentTask').taskId)))
            || localStorage.getItem('lastPublishedTaskId');
        return stored || null;
    } catch (e) {
        console.warn('解析任务ID失败', e);
        return null;
    }
}

// 设置事件监听器
function setupEventListeners() {
    // 星级评分事件
    setupStarRating();
    
    // 标签选择事件
    setupTagSelection();
    
    // 文字评价输入事件
    setupCommentInput();
    
    // 匿名评价切换事件
    setupAnonymousToggle();
}

// 设置星级评分
function setupStarRating() {
    const stars = document.querySelectorAll('.star');
    const ratingText = document.getElementById('ratingText');
    
    stars.forEach(star => {
        star.addEventListener('click', function() {
            const value = parseInt(this.getAttribute('data-value'));
            setRating(value);
        });
        
        star.addEventListener('mouseover', function() {
            const value = parseInt(this.getAttribute('data-value'));
            highlightStars(value);
            updateRatingText(value, true);
        });
    });
    
    // 鼠标离开时恢复当前评分
    document.getElementById('starRating').addEventListener('mouseleave', function() {
        highlightStars(currentRating);
        updateRatingText(currentRating, false);
    });
}

// 设置评分
function setRating(value) {
    currentRating = value;
    highlightStars(value);
    updateRatingText(value, false);
    checkSubmitButton();
}

// 高亮显示星星
function highlightStars(value) {
    const stars = document.querySelectorAll('.star');
    
    stars.forEach(star => {
        const starValue = parseInt(star.getAttribute('data-value'));
        if (starValue <= value) {
            star.classList.add('active');
        } else {
            star.classList.remove('active');
        }
    });
}

// 更新评分文本
function updateRatingText(value, isHover) {
    const ratingText = document.getElementById('ratingText');
    let text = '';
    let className = '';
    
    if (value === 0) {
        text = '请选择评分';
        className = '';
    } else {
        const texts = {
            1: '非常不满意',
            2: '不满意',
            3: '一般',
            4: '满意',
            5: '非常满意'
        };
        
        text = texts[value] || '';
        
        // 根据评分设置颜色类
        if (value >= 4) className = 'excellent';
        else if (value === 3) className = 'average';
        else className = 'poor';
    }
    
    ratingText.textContent = text;
    ratingText.className = 'rating-text ' + (isHover ? 'hover' : className);
}

// 设置标签选择
function setupTagSelection() {
    const tags = document.querySelectorAll('.tag');
    
    tags.forEach(tag => {
        tag.addEventListener('click', function() {
            const tagValue = this.getAttribute('data-tag');
            
            if (this.classList.contains('selected')) {
                // 取消选择
                this.classList.remove('selected');
                selectedTags = selectedTags.filter(t => t !== tagValue);
            } else {
                // 选择标签
                this.classList.add('selected');
                selectedTags.push(tagValue);
            }
            
            checkSubmitButton();
        });
    });
}

// 设置文字评价输入
function setupCommentInput() {
    const commentInput = document.getElementById('commentInput');
    const charCount = document.getElementById('charCount');
    
    commentInput.addEventListener('input', function() {
        commentText = this.value.trim();
        const length = commentText.length;
        
        // 更新字符计数
        charCount.textContent = length;
        
        // 警告提示
        if (length > 450) {
            charCount.parentElement.classList.add('warning');
        } else {
            charCount.parentElement.classList.remove('warning');
        }
        
        checkSubmitButton();
    });
}

// 设置匿名评价切换
function setupAnonymousToggle() {
    const checkbox = document.getElementById('anonymousCheckbox');
    
    checkbox.addEventListener('change', function() {
        isAnonymous = this.checked;
    });
}

// 检查提交按钮状态
function checkSubmitButton() {
    const submitBtn = document.getElementById('submitBtn');
    
    // 必须选择评分才能提交
    if (currentRating > 0) {
        submitBtn.disabled = false;
    } else {
        submitBtn.disabled = true;
    }
}

// 加载任务数据
function loadTaskData() {
    const taskId = getLocal('currentEvaluationTaskId');
    if (!taskId) return;
    const task = window.TaskFlow ? TaskFlow.findTask(taskId) : null;
    const stored = (!task && window.dataStore) ? (dataStore.get('currentTask') || null) : null;
    const taskDetail = ensureEvaluationRunner(task || stored, taskId);
    currentTaskDetail = taskDetail || null;
    const taskData = {
        taskId,
        type: taskDetail?.type || '任务',
        runnerName: taskDetail?.runnerName || taskDetail?.runner || '跑腿员',
        completionTime: taskDetail?.completeTime ? new Date(taskDetail.completeTime).toLocaleString() : '刚刚',
        price: (Number(taskDetail?.price || 0)).toFixed(2)
    };

    updateTaskDisplay(taskData);
}

// 更新任务显示
function updateTaskDisplay(taskData) {
    document.querySelector('.task-title').textContent = taskData.type + '任务';
    document.querySelector('.task-runner').textContent = '跑腿员：' + taskData.runnerName;
    document.querySelector('.task-time').textContent = '完成时间：' + taskData.completionTime;
    document.querySelector('.task-price').textContent = '¥' + taskData.price;
}

// 确保模拟配送下也能注入跑腿员信息，避免评价页找不到跑腿员
function ensureEvaluationRunner(taskDetail, taskId) {
    const hasRunner = taskDetail && (taskDetail.runner || taskDetail.runnerName);
    if (hasRunner) return taskDetail;

    const fallback = (window.TaskFlow && TaskFlow.getSimulatedRunnerProfile)
        ? TaskFlow.getSimulatedRunnerProfile()
        : { id: 'sim_runner_demo', name: '模拟跑腿员' };

    const patched = {
        ...(taskDetail || {}),
        runnerId: fallback.id,
        runnerName: fallback.name,
        runner: fallback.name
    };

    try {
        if (window.TaskFlow && taskId) {
            TaskFlow.updateTask(taskId, task => task ? { ...task, runnerId: patched.runnerId, runnerName: patched.runnerName, runner: patched.runner } : task);
        }
        if (window.dataStore) {
            dataStore.set('currentTask', patched);
        }
    } catch (e) {
        console.warn('回写模拟跑腿员信息失败', e);
    }

    return patched;
}

// 提交评价
function submitEvaluation() {
    // 验证评分
    if (currentRating === 0) {
        toast('请选择评分', 'error');
        return;
    }
    currentTaskDetail = ensureEvaluationRunner(currentTaskDetail, getLocal('currentEvaluationTaskId'));
    if (!currentTaskDetail || (!currentTaskDetail.runner && !currentTaskDetail.runnerName)) {
        toast('未找到跑腿员信息，无法提交评价', 'error');
        return;
    }
    if (existingEvaluation) {
        toast('已完成评价，无需重复提交', 'info');
        return;
    }
    
    // 显示提交中状态
    const submitBtn = document.getElementById('submitBtn');
    submitBtn.textContent = '提交中...';
    submitBtn.disabled = true;
    
    // 构建评价数据
    const taskId = getLocal('currentEvaluationTaskId');
    const fallbackTask = currentTaskDetail || (window.dataStore ? dataStore.get('currentTask') : null) || {};
    const runnerId = fallbackTask.runnerId || fallbackTask.runner || 'runner';
    const runnerName = fallbackTask.runnerName || fallbackTask.runner || '跑腿员';
    const publisherId = fallbackTask.publisherId || fallbackTask.publisher || null;

    const evaluationData = {
        taskId,
        rating: currentRating,
        tags: selectedTags,
        comment: commentText,
        isAnonymous: isAnonymous,
        submitTime: new Date().toISOString(),
        userId: getLocal('authUser')?.id || 'anonymous',
        runnerId,
        runnerName,
        publisherId
    };
    
    // 模拟提交过程
    setTimeout(() => {
        // 保存评价数据
        saveEvaluation(evaluationData);
        
        // 更新跑腿员评分
        updateRunnerRating(evaluationData);

        // 回写任务评价状态，便于其他页面识别已评价
        try {
            if (window.TaskFlow && taskId) {
                TaskFlow.updateTask(taskId, task => task ? { ...task, evaluated: true, evaluation: evaluationData } : task);
            }
        } catch (e) {
            console.warn('同步评价到任务失败', e);
        }
        
        toast('评价提交成功！', 'success');
        
        // 跳转到任务记录
        setTimeout(() => {
            window.location.href = '任务记录.html?tab=accepted';
        }, 1200);
        
    }, 1000);
}

// 保存评价数据
function saveEvaluation(evaluationData) {
    // 获取现有评价数据
    const evaluations = getLocal('evaluations') || [];

    // 同一任务只保存一次，后写覆盖
    const idx = evaluations.findIndex(ev => ev.taskId === evaluationData.taskId);
    if (idx >= 0) {
        evaluations[idx] = evaluationData;
    } else {
        evaluations.push(evaluationData);
    }
    
    // 保存到本地存储
    setLocal('evaluations', evaluations);
    
    // 记录评价次数
    const evaluationStats = getLocal('evaluationStats') || {
        totalEvaluations: 0,
        averageRating: 0,
        lastEvaluation: null
    };
    
    evaluationStats.totalEvaluations++;
    evaluationStats.lastEvaluation = new Date().toISOString();
    setLocal('evaluationStats', evaluationStats);
}

// 更新跑腿员评分
function updateRunnerRating(evaluationData) {
    // 获取跑腿员数据
    const runners = getLocal('runners') || {};
    const runnerKey = evaluationData.runnerId || evaluationData.runnerName || 'runner';
    const runnerName = evaluationData.runnerName || '跑腿员';

    if (!runners[runnerKey]) {
        runners[runnerKey] = {
            name: runnerName,
            totalRatings: 0,
            averageRating: 0,
            totalEvaluations: 0,
            tags: {},
            lastEvaluation: null
        };
    }

    const runner = runners[runnerKey];
    runner.name = runnerName;
    
    // 更新评分统计
    runner.totalRatings += evaluationData.rating;
    runner.totalEvaluations++;
    runner.averageRating = runner.totalRatings / runner.totalEvaluations;
    runner.lastEvaluation = evaluationData.submitTime;
    
    // 更新标签统计
    (evaluationData.tags || []).forEach(tag => {
        if (!runner.tags[tag]) {
            runner.tags[tag] = 0;
        }
        runner.tags[tag]++;
    });
    
    // 保存更新后的数据
    setLocal('runners', runners);
}

// 返回上一页
function goBack() {
    // 检查是否有未保存的评价（已有评价时不提示）
    if (!existingEvaluation && currentRating > 0 && window.Dialog) {
        Dialog.show({
            title: '确认返回',
            content: '您有未提交的评价，确定要返回吗？',
            buttons: [
                { text: '取消', type: 'secondary' },
                { text: '确定返回', type: 'primary' }
            ]
        }).then(result => {
            if (result === '确定返回') {
                window.history.back();
            }
        });
    } else {
        window.history.back();
    }
}

// 若已有评价，填充并禁用再评
function hydrateExistingEvaluation() {
    const taskId = getLocal('currentEvaluationTaskId');
    if (!taskId) return;
    const evaluations = getLocal('evaluations') || [];
    const exist = evaluations.find(ev => ev.taskId === taskId);
    if (!exist) return;
    existingEvaluation = exist;

    currentRating = exist.rating || 0;
    selectedTags = exist.tags || [];
    commentText = exist.comment || '';
    isAnonymous = !!exist.isAnonymous;

    // 填充星级
    highlightStars(currentRating);
    updateRatingText(currentRating, false);

    // 填充标签
    document.querySelectorAll('.tag').forEach(tagEl => {
        const val = tagEl.getAttribute('data-tag');
        if (selectedTags.includes(val)) {
            tagEl.classList.add('selected');
        }
    });

    // 填充文本
    const commentInput = document.getElementById('commentInput');
    const charCount = document.getElementById('charCount');
    if (commentInput) {
        commentInput.value = commentText;
        charCount.textContent = commentText.length;
        commentInput.disabled = true;
    }

    // 填充匿名
    const checkbox = document.getElementById('anonymousCheckbox');
    if (checkbox) {
        checkbox.checked = isAnonymous;
        checkbox.disabled = true;
    }

    // 禁用星和标签
    document.querySelectorAll('.star').forEach(star => {
        star.classList.add('readonly');
        star.style.pointerEvents = 'none';
    });
    document.querySelectorAll('.tag').forEach(tag => {
        tag.style.pointerEvents = 'none';
    });

    // 禁用提交按钮
    const submitBtn = document.getElementById('submitBtn');
    if (submitBtn) {
        submitBtn.textContent = '已评价';
        submitBtn.disabled = true;
    }
}

// 页面可见性变化处理
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        // 页面重新可见时刷新数据
        loadTaskData();
    }
});

// 错误处理
window.addEventListener('error', function(e) {
    console.error('评价页面错误:', e.error);
    toast('页面加载出错，请刷新重试', 'error');
});

// 导出函数供其他页面使用
window.evaluation = {
    setRating,
    submitEvaluation,
    goBack
};