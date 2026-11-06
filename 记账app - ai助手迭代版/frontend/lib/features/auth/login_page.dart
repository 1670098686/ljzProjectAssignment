import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers/user_provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/font_theme.dart';

/// 登录页面
/// 提供用户登录功能，包含邮箱和密码输入
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  
  bool _obscurePassword = true;
  bool _isLoading = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
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
              
              // 登录表单
              _buildLoginForm(),
              
              const SizedBox(height: 24),
              
              // 登录按钮
              _buildLoginButton(),
              
              const SizedBox(height: 24),
              
              // 注册链接
              _buildRegisterLink(),
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
          context.go('/');
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
      '登录',
      style: AppTextStyles.headlineLarge.copyWith(
        color: AppColors.primary,
      ),
    );
  }

  /// 构建副标题
  Widget _buildSubtitle() {
    return Text(
      '欢迎回来，请登录您的账户',
      style: AppTextStyles.bodyMedium.copyWith(
        color: AppColors.grey600,
      ),
    );
  }

  /// 构建登录表单
  Widget _buildLoginForm() {
    return Form(
      key: _formKey,
      child: Column(
        children: [
          // 邮箱输入框
          _buildEmailField(),
          
          const SizedBox(height: 16),
          
          // 密码输入框
          _buildPasswordField(),
          
          const SizedBox(height: 16),
          
          // 忘记密码链接
          _buildForgotPasswordLink(),
        ],
      ),
    );
  }

  /// 构建邮箱输入框
  Widget _buildEmailField() {
    return TextFormField(
      controller: _emailController,
      decoration: InputDecoration(
        labelText: '邮箱',
        hintText: '请输入您的邮箱',
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
          return '请输入邮箱';
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
        hintText: '请输入您的密码',
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
          return '请输入密码';
        }
        if (value.length < 6) {
          return '密码长度不能少于6个字符';
        }
        return null;
      },
    );
  }

  /// 构建忘记密码链接
  Widget _buildForgotPasswordLink() {
    return Align(
      alignment: Alignment.centerRight,
      child: TextButton(
        onPressed: _handleForgotPassword,
        child: Text(
          '忘记密码？',
          style: TextStyle(
            color: AppColors.primary,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  /// 构建登录按钮
  Widget _buildLoginButton() {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    
    return ElevatedButton(
      onPressed: _isLoading ? null : () => _handleLogin(userProvider),
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
      child: _isLoading 
          ? const CircularProgressIndicator(color: Colors.white) 
          : const Text('登录'),
    );
  }

  /// 构建注册链接
  Widget _buildRegisterLink() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          '还没有账户？',
          style: TextStyle(
            color: AppColors.grey600,
          ),
        ),
        const SizedBox(width: 8),
        TextButton(
          onPressed: _handleRegister,
          child: Text(
            '立即注册',
            style: TextStyle(
              color: AppColors.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }

  /// 处理登录
  Future<void> _handleLogin(UserProvider userProvider) async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final success = await userProvider.login(
        _emailController.text.trim(),
        _passwordController.text,
      );

      if (success) {
        // 登录成功，跳转到首页
        if (mounted) {
          context.go('/');
        }
      } else {
        // 显示错误信息
        if (mounted && userProvider.errorMessage != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(userProvider.errorMessage!),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('登录过程中发生错误'),
            backgroundColor: Colors.red,
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

  /// 处理忘记密码
  void _handleForgotPassword() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('请通过注册邮箱联系管理员重置密码。'),
        backgroundColor: Colors.blue,
      ),
    );
  }

  /// 处理注册
  void _handleRegister() {
    context.push('/register');
  }
}