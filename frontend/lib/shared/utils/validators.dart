import 'package:flutter/material.dart';

/// 表单验证器工具类
class AppValidators {
  /// 必填验证器
  static String? requiredValidator(String? value, {String fieldName = '此项'}) {
    if (value == null || value.trim().isEmpty) {
      return '$fieldName不能为空';
    }
    return null;
  }
  
  /// 金额验证器
  static String? amountValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '金额不能为空';
    }
    
    final amount = double.tryParse(value);
    if (amount == null) {
      return '请输入有效的金额';
    }
    
    if (amount <= 0) {
      return '金额必须大于0';
    }
    
    if (amount > 1000000000) {
      return '金额不能超过10亿';
    }
    
    return null;
  }
  
  /// 分类名称验证器
  static String? categoryNameValidator(String? value) {
    final requiredError = requiredValidator(value, fieldName: '分类名称');
    if (requiredError != null) return requiredError;
    
    if (value!.length > 20) {
      return '分类名称不能超过20个字符';
    }
    
    if (!RegExp(r'^[\u4e00-\u9fa5a-zA-Z0-9\s]+$').hasMatch(value)) {
      return '分类名称只能包含中文、英文、数字和空格';
    }
    
    return null;
  }
  
  /// 备注验证器
  static String? remarkValidator(String? value) {
    if (value != null && value.length > 200) {
      return '备注不能超过200个字符';
    }
    return null;
  }
  
  /// 目标名称验证器
  static String? goalNameValidator(String? value) {
    final requiredError = requiredValidator(value, fieldName: '目标名称');
    if (requiredError != null) return requiredError;
    
    if (value!.length > 30) {
      return '目标名称不能超过30个字符';
    }
    
    if (!RegExp(r'^[\u4e00-\u9fa5a-zA-Z0-9\s]+$').hasMatch(value)) {
      return '目标名称只能包含中文、英文、数字和空格';
    }
    
    return null;
  }
  
  /// 目标金额验证器
  static String? targetAmountValidator(String? value) {
    final amountError = amountValidator(value);
    if (amountError != null) return amountError;
    
    final amount = double.parse(value!);
    if (amount < 1) {
      return '目标金额必须大于等于1元';
    }
    
    return null;
  }
  
  /// 预算金额验证器
  static String? budgetAmountValidator(String? value) {
    final amountError = amountValidator(value);
    if (amountError != null) return amountError;
    
    final amount = double.parse(value!);
    if (amount < 0.01) {
      return '预算金额必须大于等于0.01元';
    }
    
    return null;
  }
  
  /// 日期验证器
  static String? dateValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '日期不能为空';
    }
    
    try {
      final date = DateTime.parse(value);
      final now = DateTime.now();
      
      // 检查日期是否在未来（最多允许未来10年）
      final maxFutureDate = DateTime(now.year + 10, now.month, now.day);
      if (date.isAfter(maxFutureDate)) {
        return '日期不能超过10年后';
      }
      
      // 检查日期是否在过去（最多允许过去100年）
      final minPastDate = DateTime(now.year - 100, now.month, now.day);
      if (date.isBefore(minPastDate)) {
        return '日期不能超过100年前';
      }
      
    } catch (e) {
      return '请输入有效的日期格式（YYYY-MM-DD）';
    }
    
    return null;
  }
  
  /// 截止日期验证器
  static String? deadlineValidator(String? value) {
    final dateError = dateValidator(value);
    if (dateError != null) return dateError;
    
    final deadline = DateTime.parse(value!);
    final now = DateTime.now();
    
    if (deadline.isBefore(now)) {
      return '截止日期不能早于今天';
    }
    
    return null;
  }
  
  /// 邮箱验证器
  static String? emailValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '邮箱不能为空';
    }
    
    final emailRegex = RegExp(
      r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$'
    );
    
    if (!emailRegex.hasMatch(value)) {
      return '请输入有效的邮箱地址';
    }
    
    return null;
  }
  
  /// 手机号验证器
  static String? phoneValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '手机号不能为空';
    }
    
    final phoneRegex = RegExp(
      r'^1[3-9]\d{9}$'
    );
    
    if (!phoneRegex.hasMatch(value)) {
      return '请输入有效的手机号码';
    }
    
    return null;
  }
  
  /// 密码验证器
  static String? passwordValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '密码不能为空';
    }
    
    if (value.length < 6) {
      return '密码长度不能少于6位';
    }
    
    if (value.length > 20) {
      return '密码长度不能超过20位';
    }
    
    // 检查是否包含字母和数字
    final hasLetter = RegExp(r'[a-zA-Z]').hasMatch(value);
    final hasNumber = RegExp(r'[0-9]').hasMatch(value);
    
    if (!hasLetter || !hasNumber) {
      return '密码必须包含字母和数字';
    }
    
    return null;
  }
  
  /// 确认密码验证器
  static String? confirmPasswordValidator(String? value, String password) {
    if (value == null || value.trim().isEmpty) {
      return '请确认密码';
    }
    
    if (value != password) {
      return '两次输入的密码不一致';
    }
    
    return null;
  }
  
  /// 用户名验证器
  static String? usernameValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '用户名不能为空';
    }
    
    if (value.length < 2) {
      return '用户名长度不能少于2位';
    }
    
    if (value.length > 20) {
      return '用户名长度不能超过20位';
    }
    
    if (!RegExp(r'^[\u4e00-\u9fa5a-zA-Z0-9_]+$').hasMatch(value)) {
      return '用户名只能包含中文、英文、数字和下划线';
    }
    
    return null;
  }
  
  /// 身份证号验证器
  static String? idCardValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '身份证号不能为空';
    }
    
    final idCardRegex = RegExp(
      r'^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$'
    );
    
    if (!idCardRegex.hasMatch(value)) {
      return '请输入有效的身份证号码';
    }
    
    return null;
  }
  
  /// 银行卡号验证器
  static String? bankCardValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '银行卡号不能为空';
    }
    
    // 移除空格
    final cardNumber = value.replaceAll(' ', '');
    
    if (cardNumber.length < 16 || cardNumber.length > 19) {
      return '银行卡号长度应为16-19位';
    }
    
    if (!RegExp(r'^\d+$').hasMatch(cardNumber)) {
      return '银行卡号只能包含数字';
    }
    
    return null;
  }
  
  /// 百分比验证器
  static String? percentageValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '百分比不能为空';
    }
    
    final percentage = double.tryParse(value);
    if (percentage == null) {
      return '请输入有效的百分比';
    }
    
    if (percentage < 0) {
      return '百分比不能小于0';
    }
    
    if (percentage > 100) {
      return '百分比不能大于100';
    }
    
    return null;
  }
  
  /// 整数验证器
  static String? integerValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '数值不能为空';
    }
    
    final integer = int.tryParse(value);
    if (integer == null) {
      return '请输入有效的整数';
    }
    
    if (integer < 0) {
      return '数值不能小于0';
    }
    
    return null;
  }
  
  /// 正整数验证器
  static String? positiveIntegerValidator(String? value) {
    final integerError = integerValidator(value);
    if (integerError != null) return integerError;
    
    final integer = int.parse(value!);
    if (integer <= 0) {
      return '数值必须大于0';
    }
    
    return null;
  }
  
  /// 范围验证器
  static String? rangeValidator(String? value, {required double min, required double max}) {
    if (value == null || value.trim().isEmpty) {
      return '数值不能为空';
    }
    
    final number = double.tryParse(value);
    if (number == null) {
      return '请输入有效的数值';
    }
    
    if (number < min) {
      return '数值不能小于$min';
    }
    
    if (number > max) {
      return '数值不能大于$max';
    }
    
    return null;
  }
  
  /// 自定义正则验证器
  static String? regexValidator(String? value, RegExp regex, String errorMessage) {
    if (value == null || value.trim().isEmpty) {
      return null; // 空值不验证，由requiredValidator处理
    }
    
    if (!regex.hasMatch(value)) {
      return errorMessage;
    }
    
    return null;
  }
  
  /// 组合验证器
  static String? combineValidators(String? value, List<String? Function(String?)> validators) {
    for (final validator in validators) {
      final error = validator(value);
      if (error != null) {
        return error;
      }
    }
    return null;
  }
}

/// 表单字段装饰器工具类
class AppFormDecorators {
  /// 获取金额输入框装饰器
  static InputDecoration amountInputDecoration({
    String? labelText,
    String? hintText,
    String? errorText,
    Widget? prefixIcon,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText ?? '金额',
      hintText: hintText ?? '请输入金额',
      errorText: errorText,
      prefixIcon: prefixIcon ?? const Icon(Icons.attach_money),
      suffixIcon: suffixIcon,
      border: const OutlineInputBorder(),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
  
  /// 获取分类名称输入框装饰器
  static InputDecoration categoryInputDecoration({
    String? labelText,
    String? hintText,
    String? errorText,
    Widget? prefixIcon,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText ?? '分类名称',
      hintText: hintText ?? '请输入分类名称',
      errorText: errorText,
      prefixIcon: prefixIcon ?? const Icon(Icons.category),
      suffixIcon: suffixIcon,
      border: const OutlineInputBorder(),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
  
  /// 获取备注输入框装饰器
  static InputDecoration remarkInputDecoration({
    String? labelText,
    String? hintText,
    String? errorText,
    Widget? prefixIcon,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText ?? '备注',
      hintText: hintText ?? '请输入备注（可选）',
      errorText: errorText,
      prefixIcon: prefixIcon ?? const Icon(Icons.note),
      suffixIcon: suffixIcon,
      border: const OutlineInputBorder(),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
  
  /// 获取目标名称输入框装饰器
  static InputDecoration goalInputDecoration({
    String? labelText,
    String? hintText,
    String? errorText,
    Widget? prefixIcon,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText ?? '目标名称',
      hintText: hintText ?? '请输入目标名称',
      errorText: errorText,
      prefixIcon: prefixIcon ?? const Icon(Icons.flag),
      suffixIcon: suffixIcon,
      border: const OutlineInputBorder(),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
  
  /// 获取日期输入框装饰器
  static InputDecoration dateInputDecoration({
    String? labelText,
    String? hintText,
    String? errorText,
    Widget? prefixIcon,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText ?? '日期',
      hintText: hintText ?? '请选择日期',
      errorText: errorText,
      prefixIcon: prefixIcon ?? const Icon(Icons.calendar_today),
      suffixIcon: suffixIcon,
      border: const OutlineInputBorder(),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
  
  /// 获取通用输入框装饰器
  static InputDecoration generalInputDecoration({
    required String labelText,
    String? hintText,
    String? errorText,
    Widget? prefixIcon,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText,
      hintText: hintText ?? '请输入$labelText',
      errorText: errorText,
      prefixIcon: prefixIcon,
      suffixIcon: suffixIcon,
      border: const OutlineInputBorder(),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
}

/// 表单状态管理工具类
class FormStateHelpers {
  /// 检查表单是否有效
  static bool isFormValid(GlobalKey<FormState> formKey) {
    return formKey.currentState?.validate() ?? false;
  }
  
  /// 保存表单数据
  static void saveForm(GlobalKey<FormState> formKey) {
    formKey.currentState?.save();
  }
  
  /// 重置表单
  static void resetForm(GlobalKey<FormState> formKey) {
    formKey.currentState?.reset();
  }
  
  /// 验证并保存表单
  static bool validateAndSaveForm(GlobalKey<FormState> formKey) {
    if (isFormValid(formKey)) {
      saveForm(formKey);
      return true;
    }
    return false;
  }
  
  /// 显示表单错误提示
  static void showFormErrors(BuildContext context, List<String> errors) {
    if (errors.isNotEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('请检查以下错误：'),
              ...errors.map((error) => Text('• $error')),
            ],
          ),
          backgroundColor: Colors.red,
          duration: const Duration(seconds: 5),
        ),
      );
    }
  }
}