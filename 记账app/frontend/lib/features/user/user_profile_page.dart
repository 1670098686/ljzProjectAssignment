import 'dart:io';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';

class UserProfilePage extends StatefulWidget {
  const UserProfilePage({super.key});

  @override
  State<UserProfilePage> createState() => _UserProfilePageState();
}

class _UserProfilePageState extends State<UserProfilePage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _nicknameController = TextEditingController();
  final _introductionController = TextEditingController();
  final ImagePicker _imagePicker = ImagePicker();
  File? _avatarFile;

  @override
  void initState() {
    super.initState();
    _loadUserProfile();
  }

  Future<void> _loadUserProfile() async {
    // 模拟加载用户数据
    final prefs = await SharedPreferences.getInstance();
    String email = prefs.getString('profile_email') ?? '';
    // 如果之前保存的是'未设置邮箱'，则转换为空字符串
    if (email == '未设置邮箱') {
      email = '';
    }
    final nickname = prefs.getString('profile_nickname') ?? '';
    final introduction = prefs.getString('profile_introduction') ?? '';

    setState(() {
      _emailController.text = email;
      _nicknameController.text = nickname;
      _introductionController.text = introduction;
    });
  }

  Future<void> _saveUserProfile() async {
    if (_formKey.currentState!.validate()) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('profile_email', _emailController.text.trim());
      await prefs.setString('profile_nickname', _nicknameController.text.trim());
      await prefs.setString('profile_introduction', _introductionController.text.trim());

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('个人资料已保存')),
        );
        context.pop();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('编辑个人资料'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        actions: [
          TextButton(
            onPressed: _saveUserProfile,
            child: const Text('保存'),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 头像编辑区域
              _buildAvatarSection(colorScheme),
              const SizedBox(height: 32),

              // 基本信息表单
              _buildBasicInfoForm(),
              const SizedBox(height: 24),

              // 保存按钮
              _buildSaveButton(colorScheme),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAvatarSection(ColorScheme colorScheme) {
    final initials = _nicknameController.text.trim().isNotEmpty
        ? _nicknameController.text.trim()[0].toUpperCase()
        : _emailController.text.trim().isNotEmpty
            ? _emailController.text.trim()[0].toUpperCase()
            : 'U';

    return Center(
      child: Column(
        children: [
          GestureDetector(
            onTap: _showImagePickerOptions,
            child: Stack(
              children: [
                CircleAvatar(
                  radius: 50,
                  backgroundColor: colorScheme.primary,
                  backgroundImage: _avatarFile != null
                      ? FileImage(_avatarFile!)
                      : null,
                  child: _avatarFile == null
                      ? Text(
                          initials,
                          style: const TextStyle(
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        )
                      : null,
                ),
                Positioned(
                  bottom: 0,
                  right: 0,
                  child: Container(
                    padding: const EdgeInsets.all(6),
                    decoration: BoxDecoration(
                      color: colorScheme.primary,
                      shape: BoxShape.circle,
                      border: Border.all(color: colorScheme.surface, width: 2),
                    ),
                    child: Icon(
                      Icons.camera_alt,
                      size: 18,
                      color: colorScheme.onPrimary,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          TextButton.icon(
            onPressed: _showImagePickerOptions,
            icon: const Icon(Icons.camera_alt_outlined),
            label: const Text('更换头像'),
          ),
        ],
      ),
    );
  }

  Widget _buildBasicInfoForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '基本信息',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 16),



        // 昵称输入框
        TextFormField(
          controller: _nicknameController,
          decoration: const InputDecoration(
            labelText: '昵称',
            hintText: '请输入您的昵称（可选）',
            prefixIcon: Icon(Icons.face_outlined),
          ),
        ),
        const SizedBox(height: 16),

        // 邮箱输入框
        TextFormField(
          controller: _emailController,
          decoration: const InputDecoration(
            labelText: '邮箱',
            hintText: '请输入您的邮箱地址（可选）',
            prefixIcon: Icon(Icons.email_outlined),
          ),
          keyboardType: TextInputType.emailAddress,
          validator: (value) {
            if (value != null && value.trim().isNotEmpty) {
              if (!RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(value)) {
                return '请输入有效的邮箱地址';
              }
            }
            return null;
          },
        ),
        const SizedBox(height: 16),

        // 个人介绍输入框
        TextFormField(
          controller: _introductionController,
          decoration: const InputDecoration(
            labelText: '个人介绍',
            hintText: '请输入您的个人介绍（可选）',
            prefixIcon: Icon(Icons.description_outlined),
          ),
          maxLines: 3,
          keyboardType: TextInputType.multiline,
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  Widget _buildSaveButton(ColorScheme colorScheme) {
    return SizedBox(
      width: double.infinity,
      child: FilledButton(
        onPressed: _saveUserProfile,
        style: FilledButton.styleFrom(
          padding: const EdgeInsets.symmetric(vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
        child: const Text(
          '保存修改',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
    );
  }

  // 显示图片选择选项
  Future<void> _showImagePickerOptions() async {
    final result = await showModalBottomSheet<int>(
      context: context,
      builder: (BuildContext context) {
        return SafeArea(
          child: Wrap(
            children: [
              ListTile(
                leading: const Icon(Icons.photo_library),
                title: const Text('从相册选择'),
                onTap: () => Navigator.of(context).pop(1),
              ),
              ListTile(
                leading: const Icon(Icons.camera_alt),
                title: const Text('拍照'),
                onTap: () => Navigator.of(context).pop(2),
              ),
              ListTile(
                leading: const Icon(Icons.delete),
                title: const Text('删除头像'),
                onTap: () => Navigator.of(context).pop(3),
              ),
            ],
          ),
        );
      },
    );

    if (result == 1) {
      await _pickImageFromGallery();
    } else if (result == 2) {
      await _pickImageFromCamera();
    } else if (result == 3) {
      _removeAvatar();
    }
  }

  // 从相册选择图片
  Future<void> _pickImageFromGallery() async {
    try {
      final XFile? image = await _imagePicker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 800,
        maxHeight: 800,
        imageQuality: 80,
      );
      
      if (image != null) {
        setState(() {
          _avatarFile = File(image.path);
        });
      }
    } catch (e) {
      _showErrorSnackBar('选择图片失败: $e');
    }
  }

  // 拍照
  Future<void> _pickImageFromCamera() async {
    try {
      final XFile? image = await _imagePicker.pickImage(
        source: ImageSource.camera,
        maxWidth: 800,
        maxHeight: 800,
        imageQuality: 80,
      );
      
      if (image != null) {
        setState(() {
          _avatarFile = File(image.path);
        });
      }
    } catch (e) {
      _showErrorSnackBar('拍照失败: $e');
    }
  }

  // 删除头像
  void _removeAvatar() {
    setState(() {
      _avatarFile = null;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('头像已删除')),
    );
  }

  // 显示错误提示
  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  @override
  void dispose() {
    _emailController.dispose();
    _nicknameController.dispose();
    _introductionController.dispose();
    super.dispose();
  }
}