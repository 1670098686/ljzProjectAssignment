import 'dart:io';
import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';
import 'package:screenshot/screenshot.dart';
import 'package:path_provider/path_provider.dart';

/// 分享服务类
/// 提供系统分享、图片分享、文本分享等功能
class ShareService {
  static final ShareService _instance = ShareService._internal();
  factory ShareService() => _instance;
  ShareService._internal();

  final ScreenshotController _screenshotController = ScreenshotController();

  /// 生成分享文本内容
  static String generateShareText({
    required String goalName,
    required double targetAmount,
    String? customMessage,
  }) {
    final currentDate = DateTime.now();
    final dateStr =
        '${currentDate.year}年${currentDate.month}月${currentDate.day}日';

    String message = customMessage ?? '我刚刚完成了一个储蓄目标！';

    return '''$message

🎯 储蓄目标：$goalName
💰 目标金额：¥${targetAmount.toStringAsFixed(2)}
📅 完成日期：$dateStr

通过这个应用，我成功实现了我的财务目标！强烈推荐给大家使用！''';
  }

  /// 分享纯文本
  Future<void> shareText({
    required BuildContext context,
    required String text,
    String? subject,
  }) async {
    try {
      await SharePlus.instance.share(ShareParams(text: text, subject: subject));
    } catch (e) {
      if (!context.mounted) {
        return;
      }
      _showErrorSnackBar(context, '分享失败：$e');
    }
  }

  /// 分享带图片的内容（通过截图实现）
  Future<void> shareWithImage({
    required BuildContext context,
    required Widget imageWidget,
    required String text,
    String? subject,
  }) async {
    try {
      // 显示加载提示
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('正在生成分享图片...'),
          duration: Duration(seconds: 1),
        ),
      );

      // 截取图片
      final image = await _screenshotController.captureFromWidget(
        imageWidget,
        delay: const Duration(milliseconds: 500),
        pixelRatio: 2.0,
      );

      // 保存图片到临时文件
      final directory = await getTemporaryDirectory();
      final imagePath = '${directory.path}/saving_goal_celebration.png';
      final file = File(imagePath);
      await file.writeAsBytes(image);

      // 分享图片和文本
      await SharePlus.instance.share(
        ShareParams(files: [XFile(imagePath)], text: text, subject: subject),
      );

      // 清理临时文件
      await file.delete();
    } catch (e) {
      if (!context.mounted) {
        return;
      }
      _showErrorSnackBar(context, '分享失败：$e');
    }
  }

  /// 创建分享卡片
  Widget createShareCard({
    required String goalName,
    required double targetAmount,
    String? customMessage,
  }) {
    return Container(
      width: 300,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF4CAF50), Color(0xFF2196F3)],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(77),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 庆祝图标
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(51),
              shape: BoxShape.circle,
              border: Border.all(color: Colors.white, width: 3),
            ),
            child: const Icon(Icons.celebration, size: 40, color: Colors.white),
          ),

          const SizedBox(height: 16),

          // 标题
          const Text(
            '🎉 储蓄目标达成！',
            style: TextStyle(
              color: Colors.white,
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 8),

          // 目标名称
          Text(
            goalName,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 8),

          // 金额信息
          Text(
            '¥${targetAmount.toStringAsFixed(2)}',
            style: const TextStyle(
              color: Colors.yellow,
              fontSize: 24,
              fontWeight: FontWeight.bold,
            ),
          ),

          const SizedBox(height: 16),

          // 分享文字
          const Text(
            '通过财务管理应用达成目标！',
            style: TextStyle(color: Colors.white, fontSize: 14),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  /// 显示分享选项对话框
  Future<void> showShareOptions({
    required BuildContext context,
    required String goalName,
    required double targetAmount,
    String? customMessage,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 16),
              // 标题
              const Text(
                '分享成就',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 20),

              // 分享选项
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  // 纯文本分享
                  _ShareOptionButton(
                    icon: Icons.text_fields,
                    label: '纯文本',
                    onTap: () async {
                      Navigator.pop(context);
                      final text = generateShareText(
                        goalName: goalName,
                        targetAmount: targetAmount,
                        customMessage: customMessage,
                      );
                      await shareText(
                        context: context,
                        text: text,
                        subject: '储蓄目标达成',
                      );
                    },
                  ),

                  // 图片分享
                  _ShareOptionButton(
                    icon: Icons.image,
                    label: '图片分享',
                    onTap: () async {
                      Navigator.pop(context);
                      final shareCard = createShareCard(
                        goalName: goalName,
                        targetAmount: targetAmount,
                        customMessage: customMessage,
                      );
                      final text = generateShareText(
                        goalName: goalName,
                        targetAmount: targetAmount,
                        customMessage: customMessage,
                      );
                      await shareWithImage(
                        context: context,
                        imageWidget: shareCard,
                        text: text,
                        subject: '储蓄目标达成',
                      );
                    },
                  ),

                  // 自定义分享
                  _ShareOptionButton(
                    icon: Icons.edit,
                    label: '自定义',
                    onTap: () async {
                      Navigator.pop(context);
                      await _showCustomShareDialog(
                        context,
                        goalName,
                        targetAmount,
                        customMessage,
                      );
                    },
                  ),
                ],
              ),

              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  /// 显示自定义分享对话框
  Future<void> _showCustomShareDialog(
    BuildContext context,
    String goalName,
    double targetAmount,
    String? existingMessage,
  ) async {
    final TextEditingController controller = TextEditingController(
      text: existingMessage ?? '我刚刚完成了一个储蓄目标！',
    );

    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('自定义分享消息'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('输入您想要分享的个性化消息：'),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              decoration: const InputDecoration(
                hintText: '例如：我通过努力实现了我的第一个储蓄目标！',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              final text = generateShareText(
                goalName: goalName,
                targetAmount: targetAmount,
                customMessage: controller.text.trim(),
              );
              await shareText(context: context, text: text, subject: '储蓄目标达成');
            },
            child: const Text('分享'),
          ),
        ],
      ),
    );
  }

  /// 显示错误提示
  void _showErrorSnackBar(BuildContext context, String message) {
    if (!context.mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }
}

/// 分享选项按钮组件
class _ShareOptionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _ShareOptionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          children: [
            Icon(icon, size: 32, color: Theme.of(context).primaryColor),
            const SizedBox(height: 8),
            Text(
              label,
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            ),
          ],
        ),
      ),
    );
  }
}
