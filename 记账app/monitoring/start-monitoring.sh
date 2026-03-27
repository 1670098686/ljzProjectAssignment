# 监控堆栈启动脚本
# 个人收支记账APP监控系统

#!/bin/bash

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 显示标题
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    个人收支记账APP监控堆栈启动脚本    ${NC}"
echo -e "${BLUE}========================================${NC}"

# 检查Docker和Docker Compose是否安装
check_dependencies() {
    echo -e "${YELLOW}检查依赖项...${NC}"
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: Docker未安装或不在PATH中${NC}"
        echo "请先安装Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}错误: Docker Compose未安装${NC}"
        echo "请先安装Docker Compose: https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    echo -e "${GREEN}✅ 依赖项检查通过${NC}"
}

# 创建必要目录
create_directories() {
    echo -e "${YELLOW}创建必要目录...${NC}"
    
    mkdir -p logs/prometheus
    mkdir -p logs/grafana
    mkdir -p logs/alertmanager
    mkdir -p logs/mysql
    mkdir -p logs/finance-app
    
    # 创建权限
    chmod 755 logs/*
    
    echo -e "${GREEN}✅ 目录创建完成${NC}"
}

# 启动监控服务
start_monitoring() {
    echo -e "${YELLOW}启动监控堆栈...${NC}"
    
    # 停止现有服务
    echo "停止现有服务..."
    docker-compose -f monitoring/docker-compose.monitoring.yml down --remove-orphans 2>/dev/null || true
    
    # 启动服务
    echo "启动监控服务..."
    docker-compose -f monitoring/docker-compose.monitoring.yml up -d
    
    echo -e "${GREEN}✅ 监控服务启动完成${NC}"
}

# 等待服务启动
wait_for_services() {
    echo -e "${YELLOW}等待服务启动...${NC}"
    
    local services=("prometheus:9090" "grafana:3000" "alertmanager:9093")
    
    for service in "${services[@]}"; do
        local name=$(echo $service | cut -d: -f1)
        local port=$(echo $service | cut -d: -f2)
        
        echo "检查 $name 服务..."
        for i in {1..30}; do
            if curl -s http://localhost:$port > /dev/null 2>&1; then
                echo -e "${GREEN}✅ $name 服务已就绪${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo -e "${RED}❌ $name 服务启动超时${NC}"
                return 1
            fi
            sleep 2
        done
    done
}

# 显示访问信息
show_access_info() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}监控堆栈启动成功！${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo
    echo -e "${YELLOW}服务访问地址:${NC}"
    echo -e "  📊 Prometheus:     ${GREEN}http://localhost:9090${NC}"
    echo -e "  📈 Grafana:        ${GREEN}http://localhost:3000${NC}"
    echo -e "  🔔 Alertmanager:   ${GREEN}http://localhost:9093${NC}"
    echo -e "  🔍 Jaeger:         ${GREEN}http://localhost:16686${NC}"
    echo -e "  📋 Loki:           ${GREEN}http://localhost:3100${NC}"
    echo
    echo -e "${YELLOW}默认登录信息:${NC}"
    echo -e "  Grafana用户名: ${GREEN}admin${NC}"
    echo -e "  Grafana密码:   ${GREEN}admin123${NC}"
    echo
    echo -e "${YELLOW}仪表板信息:${NC}"
    echo -e "  📊 系统概览:     系统整体状态监控"
    echo -e "  📈 业务监控:     应用业务指标监控"
    echo
    echo -e "${YELLOW}测试API调用:${NC}"
    echo -e "  健康检查: ${GREEN}curl http://localhost:8080/actuator/health${NC}"
    echo -e "  指标获取: ${GREEN}curl http://localhost:8080/actuator/prometheus${NC}"
    echo
    echo -e "${YELLOW}常用命令:${NC}"
    echo -e "  查看服务状态: ${GREEN}docker-compose -f monitoring/docker-compose.monitoring.yml ps${NC}"
    echo -e "  查看服务日志: ${GREEN}docker-compose -f monitoring/docker-compose.monitoring.yml logs -f [service]${NC}"
    echo -e "  停止监控服务: ${GREEN}docker-compose -f monitoring/docker-compose.monitoring.yml down${NC}"
}

# 主函数
main() {
    check_dependencies
    create_directories
    start_monitoring
    wait_for_services
    show_access_info
}

# 执行主函数
main "$@"