# 创建各种环境配置文件
import os
from pathlib import Path

def create_env_files():
    """创建各种环境配置文件"""
    
    # 开发环境配置
    dev_config = """# 开发环境配置
SPRING_PROFILES_ACTIVE=dev

# 数据库配置
SPRING_DATASOURCE_MASTER_URL=jdbc:mysql://localhost:3306/main_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_MASTER_USERNAME=main_user
SPRING_DATASOURCE_MASTER_PASSWORD=main_password123

SPRING_DATASOURCE_SLAVE_URL=jdbc:mysql://localhost:3308/main_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_SLAVE_USERNAME=main_user
SPRING_DATASOURCE_SLAVE_PASSWORD=main_password123

SPRING_DATASOURCE_DRIVER-CLASS-NAME=com.mysql.cj.jdbc.Driver

# Redis配置
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_DATABASE=0
SPRING_REDIS_TIMEOUT=2000ms

# RabbitMQ配置
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=finance_user
SPRING_RABBITMQ_PASSWORD=finance_password123
SPRING_RABBITMQ_VIRTUAL-HOST=finance-vhost

# Eureka配置
EUREKA_CLIENT_SERVICE-URL_DEFAULTZONE=http://localhost:8761/eureka/
EUREKA_INSTANCE_HOSTNAME=localhost

# 日志配置
LOGGING_LEVEL_COM_FINANCE=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=INFO
LOGGING_PATTERN_CONSOLE=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# 管理端点
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW-DETAILS=always
"""
    
    # 测试环境配置
    test_config = """# 测试环境配置
SPRING_PROFILES_ACTIVE=test

# 数据库配置
SPRING_DATASOURCE_MASTER_URL=jdbc:mysql://test-mysql:3306/main_db_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_MASTER_USERNAME=test_user
SPRING_DATASOURCE_MASTER_PASSWORD=test_password123

SPRING_DATASOURCE_SLAVE_URL=jdbc:mysql://test-mysql-slave:3306/main_db_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_SLAVE_USERNAME=test_user
SPRING_DATASOURCE_SLAVE_PASSWORD=test_password123

SPRING_DATASOURCE_DRIVER-CLASS-NAME=com.mysql.cj.jdbc.Driver

# Redis配置
SPRING_REDIS_HOST=test-redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_DATABASE=1

# RabbitMQ配置
SPRING_RABBITMQ_HOST=test-rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=test_user
SPRING_RABBITMQ_PASSWORD=test_password123

# Eureka配置
EUREKA_CLIENT_SERVICE-URL_DEFAULTZONE=http://test-registry:8761/eureka/

# 日志配置
LOGGING_LEVEL_COM_FINANCE=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=WARN
"""
    
    # 生产环境配置
    prod_config = """# 生产环境配置
SPRING_PROFILES_ACTIVE=prod

# 数据库配置
SPRING_DATASOURCE_MASTER_URL=jdbc:mysql://prod-mysql-master:3306/main_db_prod?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_MASTER_USERNAME=${DB_MASTER_USERNAME}
SPRING_DATASOURCE_MASTER_PASSWORD=${DB_MASTER_PASSWORD}

SPRING_DATASOURCE_SLAVE_URL=jdbc:mysql://prod-mysql-slave:3306/main_db_prod?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_SLAVE_USERNAME=${DB_SLAVE_USERNAME}
SPRING_DATASOURCE_SLAVE_PASSWORD=${DB_SLAVE_PASSWORD}

SPRING_DATASOURCE_DRIVER-CLASS-NAME=com.mysql.cj.jdbc.Driver
SPRING_DATASOURCE_HIKARI_MINIMUM-IDLE=10
SPRING_DATASOURCE_HIKARI_MAXIMUM-POOL-SIZE=50
SPRING_DATASOURCE_HIKARI_CONNECTION-TIMEOUT=20000

# Redis配置
SPRING_REDIS_HOST=${REDIS_HOST}
SPRING_REDIS_PORT=6379
SPRING_REDIS_DATABASE=2
SPRING_REDIS_PASSWORD=${REDIS_PASSWORD}
SPRING_REDIS_SSL=true

# RabbitMQ配置
SPRING_RABBITMQ_HOST=${RABBITMQ_HOST}
SPRING_RABBITMQ_PORT=5671
SPRING_RABBITMQ_USERNAME=${RABBITMQ_USERNAME}
SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
SPRING_RABBITMQ_VIRTUAL-HOST=${RABBITMQ_VHOST}
SPRING_RABBITMQ_SSL_ENABLED=true

# Eureka配置
EUREKA_CLIENT_SERVICE-URL_DEFAULTZONE=${EUREKA_SERVER_URL}
EUREKA_INSTANCE_HOSTNAME=${HOSTNAME}

# SSL配置
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY-STORE=${SSL_KEYSTORE_PATH}
SERVER_SSL_KEY-STORE-PASSWORD=${SSL_KEYSTORE_PASSWORD}
SERVER_SSL_KEY-STORE-TYPE=PKCS12
SERVER_SSL_KEY-ALIAS=${SSL_KEY_ALIAS}

# 日志配置
LOGGING_LEVEL_COM_FINANCE=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=WARN
LOGGING_LEVEL_ORG_HIBERNATE_SQL=ERROR
LOGGING_PATTERN_FILE=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
LOGGING_FILE_NAME=/var/log/finance/app.log
LOGGING_FILE_MAX-SIZE=100MB
LOGGING_FILE_MAX-HISTORY=30

# 管理端点
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW-DETAILS=when-authorized
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
"""
    
    # Docker环境配置
    docker_config = """# Docker环境配置
SPRING_PROFILES_ACTIVE=docker

# 数据库配置
SPRING_DATASOURCE_MASTER_URL=jdbc:mysql://mysql-master:3306/main_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_MASTER_USERNAME=main_user
SPRING_DATASOURCE_MASTER_PASSWORD=main_password123

SPRING_DATASOURCE_SLAVE_URL=jdbc:mysql://mysql-slave:3306/main_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_SLAVE_USERNAME=main_user
SPRING_DATASOURCE_SLAVE_PASSWORD=main_password123

SPRING_DATASOURCE_DRIVER-CLASS-NAME=com.mysql.cj.jdbc.Driver

# Redis配置
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_DATABASE=0

# RabbitMQ配置
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=finance_user
SPRING_RABBITMQ_PASSWORD=finance_password123

# Eureka配置
EUREKA_CLIENT_SERVICE-URL_DEFAULTZONE=http://finance-registry:8761/eureka/

# 日志配置
LOGGING_LEVEL_COM_FINANCE=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=WARN
LOGGING_PATTERN_CONSOLE=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
"""
    
    # 创建config目录
    config_dir = Path("config")
    config_dir.mkdir(exist_ok=True)
    
    # 写入配置文件
    configs = {
        ".env.development": dev_config,
        ".env.test": test_config,
        ".env.production": prod_config,
        ".env.docker": docker_config
    }
    
    for filename, content in configs.items():
        with open(config_dir / filename, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"✅ 已创建配置文件: config/{filename}")
    
    print("✨ 所有环境配置文件创建完成!")

def create_deployment_scripts():
    """创建部署脚本"""
    
    # 一键启动脚本
    start_script = """#!/bin/bash

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
"""

    # 一键停止脚本
    stop_script = """#!/bin/bash

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
"""

    # 状态检查脚本
    status_script = """#!/bin/bash

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
"""

    # 备份脚本
    backup_script = """#!/bin/bash

# 财务系统微服务数据备份脚本

BACKUP_DIR="./backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "💾 开始数据备份..."
echo "备份目录: $BACKUP_DIR"

# 备份MySQL数据库
echo "📊 备份MySQL数据库..."

# 备份主数据库
docker exec finance-mysql-master mysqldump -u root -proot_password123 main_db > "$BACKUP_DIR/main_db.sql"

# 备份统计数据库  
docker exec finance-mysql-statistics mysqldump -u root -proot_password123 statistics_db > "$BACKUP_DIR/statistics_db.sql"

# 备份预警数据库
docker exec finance-mysql-alert mysqldump -u root -proot_password123 alert_db > "$BACKUP_DIR/alert_db.sql"

# 备份Redis数据
echo "💾 备份Redis数据..."
docker exec finance-redis redis-cli --rdb /data/dump.rdb
docker cp finance-redis:/data/dump.rdb "$BACKUP_DIR/redis_dump.rdb"

# 备份配置文件
echo "📋 备份配置文件..."
cp docker-compose.yml "$BACKUP_DIR/"
cp -r config/ "$BACKUP_DIR/"
cp -r microservices/ "$BACKUP_DIR/" || true

# 创建备份压缩包
echo "🗜️  创建备份压缩包..."
tar -czf "$BACKUP_DIR.tar.gz" -C "./backups" "$(basename "$BACKUP_DIR")"
rm -rf "$BACKUP_DIR"

echo "✅ 数据备份完成!"
echo "备份文件: $BACKUP_DIR.tar.gz"

# 清理旧备份(保留最近7天)
find ./backups -name "*.tar.gz" -mtime +7 -delete 2>/dev/null || true
echo "🧹 已清理7天前的备份文件"
"""

    # 创建scripts目录
    scripts_dir = Path("scripts")
    scripts_dir.mkdir(exist_ok=True)
    
    # 写入脚本文件
    scripts = {
        "start.sh": start_script,
        "stop.sh": stop_script,
        "status.sh": status_script,
        "backup.sh": backup_script
    }
    
    for filename, content in scripts.items():
        script_path = scripts_dir / filename
        with open(script_path, 'w', encoding='utf-8') as f:
            f.write(content)
        # 设置可执行权限
        os.chmod(script_path, 0o755)
        print(f"✅ 已创建脚本: scripts/{filename}")
    
    print("✨ 所有部署脚本创建完成!")

def create_k8s_configs():
    """创建Kubernetes配置文件"""
    
    # 创建k8s目录
    k8s_dir = Path("k8s")
    k8s_dir.mkdir(exist_ok=True)
    
    # 命名空间
    namespace_yaml = """apiVersion: v1
kind: Namespace
metadata:
  name: finance-system
  labels:
    name: finance-system
    environment: production
"""
    
    # MySQL部署
    mysql_yaml = """apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql-master
  namespace: finance-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql-master
  template:
    metadata:
      labels:
        app: mysql-master
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: root-password
        - name: MYSQL_DATABASE
          value: "main_db"
        - name: MYSQL_USER
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: username
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        ports:
        - containerPort: 3306
        volumeMounts:
        - name: mysql-storage
          mountPath: /var/lib/mysql
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
      volumes:
      - name: mysql-storage
        persistentVolumeClaim:
          claimName: mysql-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: mysql-master
  namespace: finance-system
spec:
  selector:
    app: mysql-master
  ports:
  - port: 3306
    targetPort: 3306
  type: ClusterIP
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: finance-system
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 50Gi
"""
    
    # 主服务部署
    main_service_yaml = """apiVersion: apps/v1
kind: Deployment
metadata:
  name: finance-main-service
  namespace: finance-system
  labels:
    app: finance-main-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: finance-main-service
  template:
    metadata:
      labels:
        app: finance-main-service
    spec:
      containers:
      - name: main-service
        image: finance/main-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: SPRING_DATASOURCE_MASTER_URL
          value: "jdbc:mysql://mysql-master:3306/main_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
        - name: SPRING_DATASOURCE_MASTER_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_MASTER_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        - name: SPRING_REDIS_HOST
          value: "redis-service"
        - name: EUREKA_CLIENT_SERVICE-URL_DEFAULTZONE
          value: "http://finance-registry:8761/eureka/"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
      imagePullSecrets:
      - name: registry-secret
---
apiVersion: v1
kind: Service
metadata:
  name: finance-main-service
  namespace: finance-system
spec:
  selector:
    app: finance-main-service
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
"""
    
    # API网关部署
    gateway_yaml = """apiVersion: apps/v1
kind: Deployment
metadata:
  name: finance-gateway
  namespace: finance-system
  labels:
    app: finance-gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: finance-gateway
  template:
    metadata:
      labels:
        app: finance-gateway
    spec:
      containers:
      - name: gateway
        image: finance/finance-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: SPRING_REDIS_HOST
          value: "redis-service"
        - name: EUREKA_CLIENT_SERVICE-URL_DEFAULTZONE
          value: "http://finance-registry:8761/eureka/"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      imagePullSecrets:
      - name: registry-secret
---
apiVersion: v1
kind: Service
metadata:
  name: finance-gateway
  namespace: finance-system
spec:
  selector:
    app: finance-gateway
  ports:
  - port: 8080
    targetPort: 8080
  type: LoadBalancer
"""
    
    k8s_configs = {
        "00-namespace.yaml": namespace_yaml,
        "01-mysql.yaml": mysql_yaml,
        "02-main-service.yaml": main_service_yaml,
        "03-gateway.yaml": gateway_yaml
    }
    
    for filename, content in k8s_configs.items():
        with open(k8s_dir / filename, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"✅ 已创建K8s配置: k8s/{filename}")
    
    print("✨ 所有Kubernetes配置创建完成!")

def main():
    """主函数"""
    print("🚀 开始创建部署配置文件...")
    
    create_env_files()
    create_deployment_scripts()
    create_k8s_configs()
    
    print("")
    print("🎉 所有部署配置文件创建完成!")
    print("")
    print("📁 创建的文件:")
    print("├── config/                    # 环境配置文件")
    print("│   ├── .env.development")
    print("│   ├── .env.test") 
    print("│   ├── .env.production")
    print("│   └── .env.docker")
    print("├── scripts/                   # 部署脚本")
    print("│   ├── start.sh")
    print("│   ├── stop.sh")
    print("│   ├── status.sh")
    print("│   └── backup.sh")
    print("├── k8s/                       # Kubernetes配置")
    print("│   ├── 00-namespace.yaml")
    print("│   ├── 01-mysql.yaml")
    print("│   ├── 02-main-service.yaml")
    print("│   └── 03-gateway.yaml")
    print("├── docker-compose.yml")
    print("└── generate-dockerfiles.py")

if __name__ == "__main__":
    main()