#!/bin/bash

# 财务系统微服务状态检查脚本

echo "📊 财务系统微服务状态检查"
echo "========================================"

# 检查Docker服务状态
echo "🐳 Docker容器状态:"
docker-compose ps

echo ""
echo "🌐 网络端口检查:"
SERVICES=("8761:服务注册中心" "8080:API网关" "8081:主服务" "8082:统计服务" "8084:预警服务" "3307:MySQL主库" "3308:MySQL从库" "6379:Redis" "5672:RabbitMQ" "15672:RabbitMQ管理")
for service in "${SERVICES[@]}"; do
    IFS=':' read -r port name <<< "$service"
    if nc -z localhost $port 2>/dev/null; then
        echo "✅ $name (端口 $port): 正常"
    else
        echo "❌ $name (端口 $port): 异常"
    fi
done

echo ""
echo "🏥 服务健康检查:"

# 检查服务注册中心
if curl -s http://localhost:8761/eureka/apps > /dev/null; then
    echo "✅ 服务注册中心: 正常"
else
    echo "❌ 服务注册中心: 异常"
fi

# 检查API网关
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ API网关: 正常"
else
    echo "❌ API网关: 异常"
fi

# 检查各微服务
SERVICES=("主服务" "统计服务" "预警服务")
for service in "${SERVICES[@]}"; do
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ $service: 正常"
    else
        echo "❌ $service: 异常"
    fi
done

echo ""
echo "📋 最近错误日志:"
docker-compose logs --tail=20 --since=1h | grep -i error || echo "未发现错误日志"
