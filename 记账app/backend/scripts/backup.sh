#!/bin/bash

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
