import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers/user_provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/font_theme.dart';

/// 注册页面
/// 提供用户注册功能，包含用户名、邮箱和密码输入
class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  bool _isLoading = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // 使用主题默认的scaffoldBackgroundColor，与首页保持一致
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 返回按钮
              _buildBackButton(),
              
              const SizedBox(height: 40),
              
              // 标题
              _buildTitle(),
              
              const SizedBox(height: 8),
              
              // 副标题
              _buildSubtitle(),
              
              const SizedBox(height: 40),
              
              // 注册表单
              _buildRegisterForm(),
              
              const SizedBox(height: 24),
              
              // 注册按钮
              _buildRegisterButton(),
              
              const SizedBox(height: 24),
              
              // 登录链接
              _buildLoginLink(),
            ],
          ),
        ),
      ),
    );
  }

  /// 构建返回按钮
  Widget _buildBackButton() {
    return IconButton(
      onPressed: () {
        if (Navigator.canPop(context)) {
          context.pop();
        } else {
          context.go('/login');
        }
      },
      icon: const Icon(
        Icons.arrow_back_ios,
        color: AppColors.onSurface,
        size: 20,
      ),
      padding: EdgeInsets.zero,
      constraints: const BoxConstraints(),
    );
  }

  /// 构建标题
  Widget _buildTitle() {
    return Text(
      '注册',
      style: AppTextStyles.headlineLarge.copyWith(
        color: AppColors.primary,
      ),
    );
  }

  /// 构建副标题
  Widget _buildSubtitle() {
    return Text(
      '创建您的账户，开始记账之旅',
      style: AppTextStyles.bodyMedium.copyWith(
        color: AppColors.grey600,
      ),
    );
  }

  /// 构建注册表单
  Widget _buildRegisterForm() {
    return Form(
      key: _formKey,
      child: Column(
        children: [
          // 用户名输入框
          _buildUsernameField(),
          
          const SizedBox(height: 16),
          
          // 邮箱输入框
          _buildEmailField(),
          
          const SizedBox(height: 16),
          
          // 密码输入框
          _buildPasswordField(),
          
          const SizedBox(height: 16),
          
          // 确认密码输入框
          _buildConfirmPasswordField(),
          
          const SizedBox(height: 16),
          
          // 用户协议
          _buildTermsAgreement(),
        ],
      ),
    );
  }

  /// 构建用户名输入框
  Widget _buildUsernameField() {
    return TextFormField(
      controller: _usernameController,
      decoration: InputDecoration(
        labelText: '用户名',
        hintText: '请输入用户名',
        prefixIcon: const Icon(Icons.person_outlined),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: AppColors.surface,
      ),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '用户名不能为空';
        }
        if (value.length < 2) {
          return '用户名长度不能少于2个字符';
        }
        if (!RegExp(r'^[a-zA-Z0-9_]+$').hasMatch(value)) {
          return '用户名只能包含字母、数字和下划线';
        }
        return null;
      },
    );
  }

  /// 构建邮箱输入框
  Widget _buildEmailField() {
    return TextFormField(
      controller: _emailController,
      decoration: InputDecoration(
        labelText: '邮箱',
        hintText: '请输入邮箱',
        prefixIcon: const Icon(Icons.email_outlined),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: AppColors.surface,
      ),
      keyboardType: TextInputType.emailAddress,
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '邮箱不能为空';
        }
        if (!RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(value)) {
          return '请输入有效的邮箱地址';
        }
        return null;
      },
    );
  }

  /// 构建密码输入框
  Widget _buildPasswordField() {
    return TextFormField(
      controller: _passwordController,
      decoration: InputDecoration(
        labelText: '密码',
        hintText: '请输入密码',
        prefixIcon: const Icon(Icons.lock_outlined),
        suffixIcon: IconButton(
          onPressed: () {
            setState(() {
              _obscurePassword = !_obscurePassword;
            });
          },
          icon: Icon(
            _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
          ),
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: AppColors.surface,
      ),
      obscureText: _obscurePassword,
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '密码不能为空';
        }
        if (value.length < 6) {
          return '密码长度不能少于6个字符';
        }
        if (!RegExp(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)').hasMatch(value)) {
          return '密码必须包含大小写字母和数字';
        }
        return null;
      },
    );
  }

  /// 构建确认密码输入框
  Widget _buildConfirmPasswordField() {
    return TextFormField(
      controller: _confirmPasswordController,
      decoration: InputDecoration(
        labelText: '确认密码',
        hintText: '请再次输入密码',
        prefixIcon: const Icon(Icons.lock_outlined),
        suffixIcon: IconButton(
          onPressed: () {
            setState(() {
              _obscureConfirmPassword = !_obscureConfirmPassword;
            });
          },
          icon: Icon(
            _obscureConfirmPassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
          ),
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: AppColors.surface,
      ),
      obscureText: _obscureConfirmPassword,
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '请再次输入密码';
        }
        if (value != _passwordController.text) {
          return '两次输入的密码不一致';
        }
        return null;
      },
    );
  }

  /// 构建用户协议
  Widget _buildTermsAgreement() {
    return Row(
      children: [
        Checkbox(
          value: true, // 默认同意协议
          onChanged: null, // 不可取消，必须同意
          activeColor: AppColors.primary,
        ),
        Expanded(
          child: RichText(
            text: TextSpan(
              style: TextStyle(
                color: AppColors.grey600,
                fontSize: 14,
              ),
              children: [
                const TextSpan(text: '我已阅读并同意'),
                TextSpan(
                  text: '《用户协议》',
                  style: TextStyle(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const TextSpan(text: '和'),
                TextSpan(
                  text: '《隐私政策》',
                  style: TextStyle(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// 构建注册按钮
  Widget _buildRegisterButton() {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    
    return ElevatedButton(
      onPressed: _isLoading ? null : () => _handleRegister(userProvider),
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
      child: _isLoading ? const CircularProgressIndicator(color: Colors.white) : const Text('注册'),
    );
  }

  /// 构建登录链接
  Widget _buildLoginLink() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          '已有账号？',
          style: TextStyle(
            color: AppColors.grey600,
          ),
        ),
        const SizedBox(width: 8),
        TextButton(
          onPressed: _handleLogin,
          child: Text(
            '立即登录',
            style: TextStyle(
              color: AppColors.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }

  /// 处理注册
  Future<void> _handleRegister(UserProvider userProvider) async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final success = await userProvider.register(
        _emailController.text.trim(),
        _passwordController.text,
        _usernameController.text.trim(),
      );

      if (success) {
        // 注册成功，跳转到首页
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: const Text('注册成功，欢迎加入我们的应用！'),
              backgroundColor: AppColors.success,
              duration: const Duration(seconds: 3),
            ),
          );
          // 使用mounted检查来避免异步操作中的BuildContext问题
          Future.delayed(const Duration(seconds: 3), () {
            if (mounted) {
              context.go('/');
            }
          });
        }
      } else {
        // 显示错误信息
        if (mounted && userProvider.errorMessage != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(userProvider.errorMessage!),
              backgroundColor: AppColors.error,
            ),
          );
        }
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('注册过程中发生错误'),
            backgroundColor: AppColors.error,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  /// 处理登录
  void _handleLogin() {
    context.push('/login');
  }
}