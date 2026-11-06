package com.finance.database.index;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 索引分析结果
 */
public class IndexAnalysisResult {
    
    private List<IndexInfo> indexInfos;
    private Map<String, QueryPerformance> queryPerformances;
    private List<OptimizationSuggestion> suggestions;
    private Date analysisTime;
    
    // Getters and Setters
    public List<IndexInfo> getIndexInfos() { return indexInfos; }
    public void setIndexInfos(List<IndexInfo> indexInfos) { this.indexInfos = indexInfos; }
    
    public Map<String, QueryPerformance> getQueryPerformances() { return queryPerformances; }
    public void setQueryPerformances(Map<String, QueryPerformance> queryPerformances) { this.queryPerformances = queryPerformances; }
    
    public List<OptimizationSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<OptimizationSuggestion> suggestions) { this.suggestions = suggestions; }
    
    public Date getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(Date analysisTime) { this.analysisTime = analysisTime; }
}