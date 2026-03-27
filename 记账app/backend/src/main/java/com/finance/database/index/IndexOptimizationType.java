package com.finance.database.index;

/**
 * 索引优化类型枚举
 */
public enum IndexOptimizationType {
    
    CREATE_INDEX("CREATE_INDEX", "创建新索引"),
    DROP_INDEX("DROP_INDEX", "删除冗余索引"),
    MODIFY_INDEX("MODIFY_INDEX", "修改索引"),
    REBUILD_INDEX("REBUILD_INDEX", "重建索引"),
    CREATE_COMPOSITE_INDEX("CREATE_COMPOSITE_INDEX", "创建复合索引"),
    REMOVE_DUPLICATE_INDEX("REMOVE_DUPLICATE_INDEX", "移除重复索引"),
    ANALYZE_TABLE("ANALYZE_TABLE", "分析表统计信息"),
    OPTIMIZE_TABLE("OPTIMIZE_TABLE", "优化表");
    
    private final String code;
    private final String description;
    
    IndexOptimizationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    
    public static IndexOptimizationType fromCode(String code) {
        for (IndexOptimizationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown optimization type: " + code);
    }
}