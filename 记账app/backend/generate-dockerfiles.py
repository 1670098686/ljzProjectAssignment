# 批量生成Dockerfile脚本
# 该脚本为所有微服务模块生成对应的Dockerfile

import os
import shutil
from pathlib import Path

# 微服务配置
MICROSERVICES = [
    {
        "name": "finance-registry",
        "port": "8761",
        "description": "服务注册中心"
    },
    {
        "name": "finance-gateway", 
        "port": "8080",
        "description": "API网关"
    },
    {
        "name": "finance-main-service",
        "port": "8081",
        "description": "主服务"
    },
    {
        "name": "finance-statistics-service",
        "port": "8082", 
        "description": "统计服务"
    },
    {
        "name": "finance-alert-service",
        "port": "8084",
        "description": "预警服务"
    }
]

def create_dockerfile(service_config):
    """为指定微服务创建Dockerfile"""
    service_name = service_config["name"]
    port = service_config["port"]
    
    # 目标目录
    service_dir = Path(f"microservices/{service_name}")
    dockerfile_path = service_dir / "Dockerfile"
    
    # 确保目录存在
    service_dir.mkdir(parents=True, exist_ok=True)
    
    # Dockerfile内容
    dockerfile_content = f"""# {service_config['description']} Dockerfile
FROM openjdk:17-jdk-slim as build

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY src ./src

# 构建应用
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# 生产镜像
FROM openjdk:17-jre-slim

# 创建应用用户
RUN groupadd -r finance && useradd -r -g finance finance

# 设置工作目录
WORKDIR /app

# 从构建镜像复制jar文件
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R finance:finance /app

# 切换到应用用户
USER finance

# 暴露端口
EXPOSE {port}

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \\
  CMD curl -f http://localhost:{port}/actuator/health || exit 1

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
"""
    
    # 写入Dockerfile
    with open(dockerfile_path, 'w', encoding='utf-8') as f:
        f.write(dockerfile_content)
    
    print(f"✅ 已创建 {service_name}/Dockerfile")

def create_service_dockerfile(service_name, port, description):
    """创建单个服务的Dockerfile"""
    service_dir = Path(f"microservices/{service_name}")
    
    if not service_dir.exists():
        print(f"⚠️  目录不存在: {service_dir}")
        return
        
    dockerfile_path = service_dir / "Dockerfile"
    
    # 跳过已存在的Dockerfile
    if dockerfile_path.exists():
        print(f"⏭️  文件已存在: {dockerfile_path}")
        return
    
    dockerfile_content = f"""# {description} Dockerfile
FROM openjdk:17-jdk-slim as builder

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY src ./src

# 安装Maven并构建
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn clean package -DskipTests

# 生产镜像
FROM openjdk:17-jre-slim

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 安装curl用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 从构建镜像复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 切换到应用用户
USER appuser

# 暴露端口
EXPOSE {port}

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \\
  CMD curl -f http://localhost:{port}/actuator/health || exit 1

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"

# 启动命令
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
"""
    
    try:
        with open(dockerfile_path, 'w', encoding='utf-8') as f:
            f.write(dockerfile_content)
        print(f"✅ 已创建 {service_name}/Dockerfile")
    except Exception as e:
        print(f"❌ 创建 {service_name}/Dockerfile 失败: {e}")

def main():
    """主函数"""
    print("🚀 开始生成微服务Dockerfile...")
    
    for service in MICROSERVICES:
        create_service_dockerfile(
            service["name"], 
            service["port"], 
            service["description"]
        )
    
    print("✨ 所有Dockerfile生成完成!")

if __name__ == "__main__":
    main()