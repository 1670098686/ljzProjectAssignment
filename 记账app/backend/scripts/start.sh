#!/bin/bash

# 财务系统微服务一键启动脚本
set -e

echo "🚀 启动财务系统微服务..."

# 检查Docker和Docker Compose
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装，请先安装Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "📁 创建目录结构..."
mkdir -p logs
mkdir -p docker/mysql/master/conf
mkdir -p docker/mysql/slave/conf
mkdir -p docker/mysql/statistics
mkdir -p docker/mysql/alert
mkdir -p docker/redis
mkdir -p docker/rabbitmq/{data,logs}
mkdir -p docker/nginx/{logs,ssl}

# 生成Docker配置文件
echo "🐳 生成Docker配置..."
python3 generate-dockerfiles.py

# 构建镜像
echo "🔨 构建Docker镜像..."
docker-compose build --no-cache

# 启动基础设施服务
echo "🏗️  启动基础设施服务..."
docker-compose up -d mysql-master mysql-slave mysql-statistics mysql-alert redis rabbitmq

# 等待数据库启动
echo "⏳ 等待数据库启动..."
sleep 30

# 启动微服务
echo "⚙️  启动微服务..."
docker-compose up -d finance-registry finance-main-service finance-statistics-service finance-alert-service

# 等待服务注册
echo "⏳ 等待服务注册..."
sleep 20

# 启动API网关和负载均衡
echo "🌐 启动API网关和负载均衡..."
docker-compose up -d finance-gateway nginx

echo "✅ 财务系统微服务启动完成!"
echo ""
echo "📊 服务访问地址:"
echo "- 服务注册中心: http://localhost:8761"
echo "- API网关: http://localhost:8080"
echo "- 主服务API: http://localhost:8080/api/v1"
echo "- 统计服务API: http://localhost:8080/api/v1/statistics"
echo "- 预警服务API: http://localhost:8080/api/v1/alerts"
echo "- RabbitMQ管理: http://localhost:15672 (finance_user/finance_password123)"
echo "- Nginx负载均衡: http://localhost:80"
echo ""
echo "🔍 查看服务状态:"
echo "  docker-compose ps"
echo "📋 查看日志:"
echo "  docker-compose logs -f [service-name]"
echo "⏹️  停止服务:"
echo "  docker-compose down"
