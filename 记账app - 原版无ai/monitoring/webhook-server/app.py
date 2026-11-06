#!/usr/bin/env python3
"""
告警处理Webhook服务器
个人收支记账APP监控系统告警处理端点
"""

from flask import Flask, request, jsonify
import logging
import json
import smtplib
from email.mime.text import MimeText
from email.mime.multipart import MimeMultipart
from datetime import datetime
import os

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 告警通知配置
NOTIFICATION_CONFIG = {
    'email': {
        'smtp_server': 'smtp.gmail.com',
        'smtp_port': 587,
        'username': os.getenv('SMTP_USERNAME', 'monitoring@finance-app.com'),
        'password': os.getenv('SMTP_PASSWORD', 'your-app-password'),
        'from_email': os.getenv('FROM_EMAIL', 'monitoring@finance-app.com'),
        'to_emails': [
            'admin@finance-app.com',
            'devops@finance-app.com'
        ]
    },
    'slack': {
        'webhook_url': os.getenv('SLACK_WEBHOOK_URL', 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK')
    }
}

@app.route('/webhook/alert', methods=['POST'])
def handle_alert():
    """处理告警Webhook"""
    try:
        data = request.get_json()
        
        if not data:
            logger.warning("收到空的告警数据")
            return jsonify({'status': 'error', 'message': 'Empty data'}), 400
        
        logger.info(f"收到告警: {data}")
        
        # 处理告警数据
        alerts = data.get('alerts', [])
        
        for alert in alerts:
            alert_type = alert.get('status', 'unknown')
            annotations = alert.get('annotations', {})
            labels = alert.get('labels', {})
            
            # 发送通知
            if alert_type == 'firing':
                send_alert_notification(alert, 'firing')
            elif alert_type == 'resolved':
                send_alert_notification(alert, 'resolved')
        
        return jsonify({
            'status': 'success',
            'message': f'处理了 {len(alerts)} 个告警'
        })
        
    except Exception as e:
        logger.error(f"处理告警时发生错误: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'service': 'alert-webhook'
    })

@app.route('/metrics', methods=['GET'])
def metrics():
    """Prometheus指标端点"""
    # 简单的运行指标
    metrics_data = f"""# HELP webhook_alerts_processed_total Total number of alerts processed
# TYPE webhook_alerts_processed_total counter
webhook_alerts_processed_total {get_alerts_processed_count()}

# HELP webhook_uptime_seconds Webhook service uptime in seconds
# TYPE webhook_uptime_seconds counter
webhook_uptime_seconds {get_uptime_seconds()}

# HELP webhook_last_alert_timestamp Last alert processing timestamp
# TYPE webhook_last_alert_timestamp counter
webhook_last_alert_timestamp {get_last_alert_timestamp()}
"""
    
    return metrics_data, 200, {'Content-Type': 'text/plain'}

def send_alert_notification(alert, status):
    """发送告警通知"""
    try:
        annotations = alert.get('annotations', {})
        labels = alert.get('labels', {})
        
        summary = annotations.get('summary', '未知告警')
        description = annotations.get('description', '无详细描述')
        
        severity = labels.get('severity', 'unknown')
        instance = labels.get('instance', 'unknown')
        service = labels.get('service', 'unknown')
        
        # 发送邮件通知
        send_email_alert(summary, description, severity, instance, service, status, alert)
        
        # 发送Slack通知 (可选)
        send_slack_alert(summary, description, severity, instance, service, status, alert)
        
        # 记录到本地日志
        log_alert_to_file(alert, status)
        
    except Exception as e:
        logger.error(f"发送告警通知时发生错误: {str(e)}")

def send_email_alert(summary, description, severity, instance, service, status, alert):
    """发送邮件告警"""
    try:
        config = NOTIFICATION_CONFIG['email']
        
        # 创建邮件内容
        subject = f"[{severity.upper()}] {summary}"
        if status == 'resolved':
            subject = f"【已恢复】{subject}"
        
        body = f"""
告警通知
{'='*50}

告警状态: {'🔴 正在发生' if status == 'firing' else '✅ 已恢复'}
告警名称: {summary}
告警级别: {severity.upper()}
服务名称: {service}
实例地址: {instance}

告警详情:
{description}

时间: {alert.get('startsAt', 'N/A')}
恢复时间: {alert.get('endsAt', 'N/A') if status == 'resolved' else 'N/A'}

建议操作:
"""
        
        # 根据严重程度添加建议
        if severity == 'critical':
            body += """
🚨 严重告警 - 请立即处理:
1. 立即检查应用服务状态
2. 查看相关错误日志
3. 联系技术支持团队
4. 考虑启用应急预案
"""
        elif severity == 'warning':
            body += """
⚠️ 警告级别 - 建议关注:
1. 关注服务运行状态
2. 检查资源使用情况
3. 准备相关应对措施
"""
        else:
            body += """
ℹ️ 信息级别 - 仅需记录:
1. 记录告警信息
2. 观察趋势变化
"""
        
        body += f"""

---
个人收支记账APP监控系统
通知时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
        """
        
        # 发送邮件
        msg = MimeMultipart()
        msg['From'] = config['from_email']
        msg['To'] = ', '.join(config['to_emails'])
        msg['Subject'] = subject
        
        msg.attach(MimeText(body, 'plain', 'utf-8'))
        
        # 注意: 实际生产环境需要正确的SMTP认证
        logger.info(f"邮件告警已准备: {subject}")
        
    except Exception as e:
        logger.error(f"发送邮件告警失败: {str(e)}")

def send_slack_alert(summary, description, severity, instance, service, status, alert):
    """发送Slack告警"""
    try:
        # 这里可以集成Slack API
        # 为简化演示，仅记录日志
        emoji = ':red_circle:' if status == 'firing' else ':white_check_mark:'
        color = 'danger' if status == 'firing' else 'good'
        
        slack_message = {
            'text': f'{emoji} {severity.upper()}: {summary}',
            'attachments': [{
                'color': color,
                'fields': [
                    {'title': '服务', 'value': service, 'short': True},
                    {'title': '实例', 'value': instance, 'short': True},
                    {'title': '状态', 'value': status, 'short': True}
                ],
                'text': description,
                'ts': int(datetime.now().timestamp())
            }]
        }
        
        logger.info(f"Slack告警已准备: {json.dumps(slack_message, ensure_ascii=False)}")
        
    except Exception as e:
        logger.error(f"发送Slack告警失败: {str(e)}")

def log_alert_to_file(alert, status):
    """记录告警到本地文件"""
    try:
        log_file = '/tmp/alerts.log'
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        log_entry = {
            'timestamp': timestamp,
            'status': status,
            'alert': alert
        }
        
        with open(log_file, 'a', encoding='utf-8') as f:
            f.write(json.dumps(log_entry, ensure_ascii=False) + '\n')
            
    except Exception as e:
        logger.error(f"记录告警到文件失败: {str(e)}")

# 全局变量用于指标统计
_alerts_processed = 0
_start_time = datetime.now()
_last_alert_time = 0

def get_alerts_processed_count():
    """获取已处理的告警数量"""
    global _alerts_processed
    return _alerts_processed

def get_uptime_seconds():
    """获取运行时间（秒）"""
    global _start_time
    return int((datetime.now() - _start_time).total_seconds())

def get_last_alert_timestamp():
    """获取最后告警时间戳"""
    global _last_alert_time
    return _last_alert_time

if __name__ == '__main__':
    logger.info("告警Webhook服务器启动...")
    logger.info("监听端口: 8080")
    logger.info("健康检查: http://localhost:8080/health")
    logger.info("指标端点: http://localhost:8080/metrics")
    logger.info("告警端点: http://localhost:8080/webhook/alert")
    
    app.run(host='0.0.0.0', port=8080, debug=True)