package com.finance.database.config;

/**
 * 数据库上下文持有器
 * 负责管理当前线程的数据库路由上下文
 */
public class DatabaseContextHolder {
    
    private static final ThreadLocal<DatabaseType> contextHolder = new ThreadLocal<>();
    
    /**
     * 设置数据库类型
     * @param databaseType 数据库类型
     */
    public static void setDatabaseType(DatabaseType databaseType) {
        if (databaseType == null) {
            throw new IllegalArgumentException("数据库类型不能为空");
        }
        contextHolder.set(databaseType);
    }
    
    /**
     * 获取当前数据库类型
     * @return 数据库类型，如果未设置则返回PRIMARY
     */
    public static DatabaseType getDatabaseType() {
        DatabaseType type = contextHolder.get();
        return (type != null) ? type : DatabaseType.PRIMARY;
    }
    
    /**
     * 清除数据库类型
     */
    public static void clearDatabaseType() {
        contextHolder.remove();
    }
    
    /**
     * 判断是否为读库
     * @return true if 当前是读库
     */
    public static boolean isReplica() {
        return getDatabaseType() == DatabaseType.REPLICA;
    }
    
    /**
     * 判断是否为主库
     * @return true if 当前是主库
     */
    public static boolean isPrimary() {
        return getDatabaseType() == DatabaseType.PRIMARY;
    }
}