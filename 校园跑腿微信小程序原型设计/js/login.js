// 登录页面功能实现

class LoginManager {
    constructor() {
        this.selectedRole = null;
        this.isCounting = false;
        this.countdown = 60;
        this.countdownTimer = null;
        this.init();
    }

    init() {
        this.bindEvents();
        this.checkAutoLogin();
    }

    bindEvents() {
        // 手机号登录表单提交
        const phoneForm = document.getElementById('phoneLoginForm');
        if (phoneForm) {
            phoneForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handlePhoneLogin();
            });
        }

        // 手机号输入实时验证
        const phoneInput = document.getElementById('phone');
        if (phoneInput) {
            phoneInput.addEventListener('input', CommonUtils.debounce(() => {
                this.validatePhoneNumber();
            }, 300));
        }

        // 验证码输入实时验证
        const codeInput = document.getElementById('code');
        if (codeInput) {
            codeInput.addEventListener('input', () => {
                this.validateVerificationCode();
            });
        }

        // 协议勾选状态检查
        const agreeCheckbox = document.getElementById('agreeCheckbox');
        if (agreeCheckbox) {
            agreeCheckbox.addEventListener('change', () => {
                this.updateLoginButtonState();
            });
        }
    }

    // 模拟微信登录
    simulateWechatLogin() {
        if (!this.checkAgreement()) {
            return;
        }

        // 显示微信授权弹窗
        modalManager.show('wechatAuthModal');
    }

    // 确认微信授权
    confirmWechatAuth() {
        modalManager.hide('wechatAuthModal');
        
        // 模拟微信登录成功
        this.handleWechatLoginSuccess();
    }

    // 微信登录成功处理
    handleWechatLoginSuccess() {
        // 模拟获取微信用户信息
        const wechatUser = {
            id: 'wx_' + CommonUtils.generateId(),
            name: '微信用户',
            avatar: '',
            phone: '',
            credit: 100,
            role: null,
            isWechatUser: true
        };

        // 保存用户信息到状态管理
        stateManager.login(wechatUser);

        // 检查是否是首次登录（没有设置角色）
        if (!wechatUser.role) {
            this.showRoleSelection();
        } else {
            this.redirectToHome();
        }
    }

    // 显示手机号登录弹窗
    showPhoneLogin() {
        if (!this.checkAgreement()) {
            return;
        }

        modalManager.show('phoneLoginModal');
        
        // 重置表单
        this.resetPhoneForm();
    }

    // 手机号登录处理
    handlePhoneLogin() {
        const phone = document.getElementById('phone').value;
        const code = document.getElementById('code').value;

        if (!this.validatePhoneNumber(phone)) {
            modalManager.showAlert('输入错误', '请输入正确的手机号码', 'error');
            return;
        }

        if (!this.validateVerificationCode(code)) {
            modalManager.showAlert('输入错误', '请输入正确的验证码', 'error');
            return;
        }

        // 模拟登录成功
        this.handlePhoneLoginSuccess(phone);
    }

    // 手机号登录成功处理
    handlePhoneLoginSuccess(phone) {
        const phoneUser = {
            id: 'phone_' + CommonUtils.generateId(),
            name: '用户' + phone.slice(-4),
            avatar: '',
            phone: phone,
            credit: 100,
            role: null,
            isPhoneUser: true
        };

        // 保存用户信息
        stateManager.login(phoneUser);
        modalManager.hide('phoneLoginModal');

        // 检查是否是首次登录
        if (!phoneUser.role) {
            this.showRoleSelection();
        } else {
            this.redirectToHome();
        }
    }

    // 显示身份选择弹窗
    showRoleSelection() {
        modalManager.show('roleSelectModal');
    }

    // 选择身份
    selectRole(role) {
        this.selectedRole = role;
        
        // 更新UI状态
        const roleCards = document.querySelectorAll('.role-card');
        roleCards.forEach(card => {
            card.classList.remove('selected');
        });
        
        const selectedCard = document.querySelector(`[onclick="selectRole('${role}')"]`);
        if (selectedCard) {
            selectedCard.classList.add('selected');
        }
        
        // 启用确认按钮
        const confirmBtn = document.getElementById('confirmRoleBtn');
        if (confirmBtn) {
            confirmBtn.disabled = false;
        }
    }

    // 确认身份选择
    confirmRoleSelection() {
        if (!this.selectedRole) {
            modalManager.showAlert('提示', '请选择身份', 'warning');
            return;
        }

        // 更新用户角色
        const currentUser = stateManager.state.currentUser;
        if (currentUser) {
            currentUser.role = this.selectedRole;
            stateManager.setState({ currentUser });
            dataStore.set('currentUser', currentUser);
        }

        modalManager.hide('roleSelectModal');
        this.redirectToHome();
    }

    // 跳过身份选择
    skipRoleSelection() {
        // 默认设置为发布者
        const currentUser = stateManager.state.currentUser;
        if (currentUser) {
            currentUser.role = 'publisher';
            stateManager.setState({ currentUser });
            dataStore.set('currentUser', currentUser);
        }

        modalManager.hide('roleSelectModal');
        this.redirectToHome();
    }

    // 发送验证码
    sendVerificationCode() {
        if (this.isCounting) {
            return;
        }

        const phone = document.getElementById('phone').value;
        if (!this.validatePhoneNumber(phone)) {
            modalManager.showAlert('输入错误', '请输入正确的手机号码', 'error');
            return;
        }

        // 开始倒计时
        this.startCountdown();

        // 模拟发送验证码
        modalManager.showAlert('验证码已发送', '验证码：123456（模拟）', 'success');
        
        // 自动填充验证码（方便测试）
        const codeInput = document.getElementById('code');
        if (codeInput) {
            codeInput.value = '123456';
            this.validateVerificationCode();
        }
    }

    // 开始倒计时
    startCountdown() {
        this.isCounting = true;
        this.countdown = 60;
        
        const sendBtn = document.getElementById('sendCodeBtn');
        if (sendBtn) {
            sendBtn.disabled = true;
        }

        this.countdownTimer = setInterval(() => {
            this.countdown--;
            
            if (sendBtn) {
                sendBtn.textContent = `重新发送(${this.countdown}s)`;
            }

            if (this.countdown <= 0) {
                this.stopCountdown();
            }
        }, 1000);
    }

    // 停止倒计时
    stopCountdown() {
        this.isCounting = false;
        
        if (this.countdownTimer) {
            clearInterval(this.countdownTimer);
            this.countdownTimer = null;
        }

        const sendBtn = document.getElementById('sendCodeBtn');
        if (sendBtn) {
            sendBtn.disabled = false;
            sendBtn.textContent = '发送验证码';
        }
    }

    // 验证手机号
    validatePhoneNumber(phone = null) {
        const phoneInput = document.getElementById('phone');
        const phoneValue = phone || phoneInput.value;
        
        const isValid = FormValidator.validatePhone(phoneValue);
        
        if (phoneInput) {
            if (phoneValue && !isValid) {
                phoneInput.classList.add('error');
            } else {
                phoneInput.classList.remove('error');
            }
        }
        
        return isValid;
    }

    // 验证验证码
    validateVerificationCode(code = null) {
        const codeInput = document.getElementById('code');
        const codeValue = code || codeInput.value;
        
        // 模拟验证码为123456
        const isValid = codeValue === '123456';
        
        if (codeInput) {
            if (codeValue && !isValid) {
                codeInput.classList.add('error');
            } else {
                codeInput.classList.remove('error');
            }
        }
        
        return isValid;
    }

    // 检查协议是否同意
    checkAgreement() {
        const agreeCheckbox = document.getElementById('agreeCheckbox');
        if (agreeCheckbox && !agreeCheckbox.checked) {
            modalManager.showAlert('提示', '请先同意用户服务协议和隐私政策', 'warning');
            return false;
        }
        return true;
    }

    // 更新登录按钮状态
    updateLoginButtonState() {
        const agreeCheckbox = document.getElementById('agreeCheckbox');
        const wechatLoginBtn = document.querySelector('.wechat-login');
        
        if (agreeCheckbox && wechatLoginBtn) {
            if (agreeCheckbox.checked) {
                wechatLoginBtn.disabled = false;
            } else {
                wechatLoginBtn.disabled = true;
            }
        }
    }

    // 重置手机号表单
    resetPhoneForm() {
        const phoneInput = document.getElementById('phone');
        const codeInput = document.getElementById('code');
        
        if (phoneInput) phoneInput.value = '';
        if (codeInput) codeInput.value = '';
        
        this.stopCountdown();
    }

    // 检查自动登录
    checkAutoLogin() {
        const currentUser = stateManager.state.currentUser;
        if (currentUser) {
            // 用户已登录，直接跳转首页
            this.redirectToHome();
        }
    }

    // 跳转到首页
    redirectToHome() {
        // 显示登录成功提示
        modalManager.showAlert('登录成功', '欢迎使用校园跑腿服务', 'success');
        
        // 延迟跳转，让用户看到提示
        setTimeout(() => {
            location.href = '首页.html';
        }, 1500);
    }
}

// 全局函数
function showPhoneLogin() {
    if (window.loginManager) {
        window.loginManager.showPhoneLogin();
    }
}

function showWechatLogin() {
    modalManager.hide('phoneLoginModal');
    modalManager.show('wechatAuthModal');
}

function simulateWechatLogin() {
    if (window.loginManager) {
        window.loginManager.simulateWechatLogin();
    }
}

function confirmWechatAuth() {
    if (window.loginManager) {
        window.loginManager.confirmWechatAuth();
    }
}

function sendVerificationCode() {
    if (window.loginManager) {
        window.loginManager.sendVerificationCode();
    }
}

function selectRole(role) {
    if (window.loginManager) {
        window.loginManager.selectRole(role);
    }
}

function confirmRoleSelection() {
    if (window.loginManager) {
        window.loginManager.confirmRoleSelection();
    }
}

function skipRoleSelection() {
    if (window.loginManager) {
        window.loginManager.skipRoleSelection();
    }
}

function showAgreement(type) {
    const titles = {
        'user': '用户服务协议',
        'privacy': '隐私政策'
    };
    
    const contents = {
        'user': '这里是用户服务协议的详细内容...',
        'privacy': '这里是隐私政策的详细内容...'
    };
    
    modalManager.showAlert(titles[type], contents[type], 'info');
}

function showOtherOptions() {
    modalManager.showAlert('其他登录方式', '目前支持微信登录和手机号登录', 'info');
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 初始化登录管理器
    window.loginManager = new LoginManager();
    
    // 添加样式（由于CSS文件可能还未加载，这里添加关键样式）
    addCriticalStyles();
    
    // 添加键盘事件支持
    addKeyboardSupport();
});

// 添加关键样式
function addCriticalStyles() {
    const style = document.createElement('style');
    style.textContent = `
        .login-main {
            min-height: 100vh;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        
        .login-hero {
            height: 200px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            text-align: center;
        }
        
        .campus-illustration h2 {
            font-size: 24px;
            margin-bottom: 8px;
            text-shadow: 0 2px 4px rgba(0,0,0,0.3);
        }
        
        .campus-illustration p {
            opacity: 0.9;
            text-shadow: 0 1px 2px rgba(0,0,0,0.3);
        }
        
        .login-form-container {
            background: white;
            border-radius: 20px 20px 0 0;
            padding: 32px 24px;
            margin-top: -20px;
            position: relative;
            z-index: 1;
        }
        
        .login-card {
            max-width: 400px;
            margin: 0 auto;
        }
        
        .login-header {
            text-align: center;
            margin-bottom: 32px;
        }
        
        .login-header h3 {
            font-size: 20px;
            margin-bottom: 8px;
            color: #262626;
        }
        
        .login-header p {
            color: #8c8c8c;
            font-size: 14px;
        }
        
        .login-method {
            margin-bottom: 24px;
        }
        
        .login-icon {
            margin-right: 8px;
            font-size: 18px;
        }
        
        .login-tip {
            text-align: center;
            font-size: 12px;
            color: #8c8c8c;
            margin-top: 8px;
        }
        
        .divider {
            text-align: center;
            margin: 24px 0;
            position: relative;
        }
        
        .divider::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 0;
            right: 0;
            height: 1px;
            background: #d9d9d9;
        }
        
        .divider span {
            background: white;
            padding: 0 16px;
            color: #8c8c8c;
            font-size: 14px;
        }
        
        .agreement {
            text-align: center;
            margin-top: 32px;
        }
        
        .checkbox-label {
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            color: #8c8c8c;
            margin-bottom: 8px;
        }
        
        .agreement-links {
            display: flex;
            justify-content: center;
            gap: 16px;
        }
        
        .agreement-links a {
            color: #1890ff;
            text-decoration: none;
            font-size: 12px;
        }
        
        .role-card {
            display: flex;
            align-items: center;
            padding: 16px;
            border: 1px solid #d9d9d9;
            border-radius: 8px;
            margin-bottom: 12px;
            cursor: pointer;
            transition: all 0.3s ease;
        }
        
        .role-card.selected {
            border-color: #1890ff;
            background: rgba(24, 144, 255, 0.05);
        }
        
        .role-icon {
            font-size: 32px;
            margin-right: 16px;
        }
        
        .role-info {
            flex: 1;
        }
        
        .role-info h4 {
            margin: 0 0 4px 0;
            font-size: 16px;
        }
        
        .role-info p {
            margin: 0;
            font-size: 14px;
            color: #8c8c8c;
        }
        
        .role-tip {
            font-size: 12px;
            color: #faad14;
            margin-top: 4px;
        }
        
        .role-check {
            color: #1890ff;
            font-size: 20px;
            opacity: 0;
            transition: opacity 0.3s ease;
        }
        
        .role-card.selected .role-check {
            opacity: 1;
        }
        
        .role-tips {
            margin-top: 16px;
            padding: 12px;
            background: #f5f5f5;
            border-radius: 6px;
            font-size: 14px;
            color: #8c8c8c;
        }
        
        .auth-content {
            text-align: center;
        }
        
        .auth-icon {
            font-size: 48px;
            margin-bottom: 16px;
        }
        
        .auth-permissions {
            text-align: left;
            margin: 16px 0;
            padding-left: 20px;
        }
        
        .auth-permissions li {
            margin-bottom: 8px;
            font-size: 14px;
        }
        
        .auth-user {
            display: flex;
            align-items: center;
            background: #f5f5f5;
            padding: 12px;
            border-radius: 8px;
            margin-top: 16px;
        }
        
        .user-avatar {
            font-size: 32px;
            margin-right: 12px;
        }
        
        .user-name {
            font-weight: 600;
        }
        
        .user-desc {
            font-size: 12px;
            color: #8c8c8c;
        }
        
        .login-options {
            display: flex;
            justify-content: space-between;
            margin-top: 16px;
        }
        
        .login-options a {
            color: #1890ff;
            text-decoration: none;
            font-size: 14px;
        }
    `;
    document.head.appendChild(style);
}

// 添加键盘支持
function addKeyboardSupport() {
    document.addEventListener('keydown', function(e) {
        // ESC键关闭弹窗
        if (e.key === 'Escape') {
            const openModal = document.querySelector('.modal.show');
            if (openModal) {
                const closeBtn = openModal.querySelector('.modal-close');
                if (closeBtn) {
                    closeBtn.click();
                }
            }
        }
        
        // Enter键在表单中提交
        if (e.key === 'Enter') {
            const activeElement = document.activeElement;
            if (activeElement && activeElement.form) {
                const submitBtn = activeElement.form.querySelector('button[type="submit"]');
                if (submitBtn && !submitBtn.disabled) {
                    submitBtn.click();
                }
            }
        }
    });
}