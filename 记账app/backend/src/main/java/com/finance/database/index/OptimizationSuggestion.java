package com.finance.database.index;

/**
 * 优化建议实体类
 */
public class OptimizationSuggestion {
    
    private IndexOptimizationType type;    // 优化类型
    private Priority priority;             // 优先级
    private String description;            // 描述
    private String tableName;              // 表名
    private String indexName;              // 索引名
    private String indexColumns;           // 索引列
    private String queryPattern;           // 查询模式
    private String expectedImprovement;    // 预期改善
    
    // Getters and Setters
    public IndexOptimizationType getType() { return type; }
    public void setType(IndexOptimizationType type) { this.type = type; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    
    public String getIndexColumns() { return indexColumns; }
    public void setIndexColumns(String indexColumns) { this.indexColumns = indexColumns; }
    
    public String getQueryPattern() { return queryPattern; }
    public void setQueryPattern(String queryPattern) { this.queryPattern = queryPattern; }
    
    public String getExpectedImprovement() { return expectedImprovement; }
    public void setExpectedImprovement(String expectedImprovement) { this.expectedImprovement = expectedImprovement; }
}