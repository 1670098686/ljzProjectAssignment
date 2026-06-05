package com.finance.database.index;

/**
 * 索引信息实体类
 */
public class IndexInfo {
    
    private String tableName;           // 表名
    private String indexName;           // 索引名
    private String columnName;          // 列名
    private int nonUnique;              // 是否唯一索引 (0=唯一, 1=非唯一)
    private int seqInIndex;             // 在复合索引中的顺序
    private long cardinality;           // 基数
    private int subPart;                // 部分索引长度
    private String nullable;            // 是否允许NULL
    private String indexType;           // 索引类型
    
    // Getters and Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    
    public int getNonUnique() { return nonUnique; }
    public void setNonUnique(int nonUnique) { this.nonUnique = nonUnique; }
    
    public int getSeqInIndex() { return seqInIndex; }
    public void setSeqInIndex(int seqInIndex) { this.seqInIndex = seqInIndex; }
    
    public long getCardinality() { return cardinality; }
    public void setCardinality(long cardinality) { this.cardinality = cardinality; }
    
    public int getSubPart() { return subPart; }
    public void setSubPart(int subPart) { this.subPart = subPart; }
    
    public String getNullable() { return nullable; }
    public void setNullable(String nullable) { this.nullable = nullable; }
    
    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) { this.indexType = indexType; }
    
    /**
     * 判断是否为唯一索引
     */
    public boolean isUnique() {
        return nonUnique == 0;
    }
    
    /**
     * 判断是否允许NULL
     */
    public boolean isNullable() {
        return "YES".equals(nullable);
    }
}