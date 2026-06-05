#!/bin/bash

# 财务系统微服务一键停止脚本
set -e

echo "⏹️  停止财务系统微服务..."

# 停止所有服务
docker-compose down

# 清理Docker卷 (可选)
read -p "是否清理Docker卷? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧹 清理Docker卷..."
    docker-compose down -v
    docker system prune -f
fi

echo "✅ 财务系统微服务已停止!"
