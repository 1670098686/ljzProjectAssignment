package com.finance.database.index;

/**
 * 优化建议优先级枚举
 */
public enum Priority {
    
    CRITICAL("CRITICAL", "紧急", 1),
    HIGH("HIGH", "高", 2),
    MEDIUM("MEDIUM", "中", 3),
    LOW("LOW", "低", 4);
    
    private final String code;
    private final String description;
    private final int level;
    
    Priority(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    
    public static Priority fromCode(String code) {
        for (Priority priority : values()) {
            if (priority.getCode().equals(code)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown priority: " + code);
    }
    
    public boolean isHigherThan(Priority other) {
        return this.level < other.level;
    }
}