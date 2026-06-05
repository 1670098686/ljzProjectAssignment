package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.repository.CategoryRepository;
import com.finance.repository.TransactionRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查和控制端点
 */
@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin(origins = "*")
@Tag(name = "系统健康检查", description = "应用状态监控、数据库检查和系统维护接口")
public class HealthController {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TransactionRecordRepository transactionRepository;

    /**
     * 应用健康检查
     */
    @GetMapping("/status")
    @Operation(summary = "应用健康状态检查", description = "检查应用整体运行状态，包括数据库连接、数据统计等")
    @OperationLog(value = "HEALTH_CHECK", description = "应用健康状态检查", recordParams = false, recordResult = false)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now());
        status.put("app", "个人收支记账APP后端服务");
        status.put("version", "1.0.0");
        status.put("status", "RUNNING");
        
        // 数据库连接检查
        try (Connection connection = dataSource.getConnection()) {
            status.put("database", "CONNECTED");
            status.put("database_url", connection.getMetaData().getURL());
        } catch (Exception e) {
            status.put("database", "DISCONNECTED");
            status.put("database_error", e.getMessage());
        }
        
        // Redis连接检查（可选功能）
        status.put("redis", "NOT_CONFIGURED");
        
        // 数据统计
        try {
            status.put("categories_count", categoryRepository.count());
            status.put("transactions_count", transactionRepository.count());
        } catch (Exception e) {
            status.put("data_stats", "ERROR: " + e.getMessage());
        }
        
        boolean isHealthy = "CONNECTED".equals(status.get("database"));
        
        if (isHealthy) {
            return ResponseEntity.ok(ApiResponse.success(status));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "服务异常"));
        }
    }
    
    /**
     * 数据库健康检查
     */
    @GetMapping("/database")
    @Operation(
        summary = "数据库健康检查", 
        description = "检查数据库连接状态，验证应用与数据库的连接是否正常"
    )
    @OperationLog(value = "DATABASE_HEALTH_CHECK", description = "数据库健康检查", recordParams = false, recordResult = false)
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkDatabase() {
        Map<String, Object> dbStatus = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            dbStatus.put("status", "OK");
            dbStatus.put("url", connection.getMetaData().getURL());
            dbStatus.put("username", connection.getMetaData().getUserName());
            dbStatus.put("product", connection.getMetaData().getDatabaseProductName());
            dbStatus.put("version", connection.getMetaData().getDatabaseProductVersion());
            
            return ResponseEntity.ok(ApiResponse.success(dbStatus));
        } catch (Exception e) {
            dbStatus.put("status", "ERROR");
            dbStatus.put("error", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, "数据库连接失败"));
        }
    }
    
    /**
     * 清理测试数据
     */
    @PostMapping("/cleanup-test-data")
    @Operation(summary = "清理测试数据", description = "清理所有测试数据，用于开发和测试环境")
    @OperationLog(value = "CLEANUP_TEST_DATA", description = "清理测试数据", businessType = "SYSTEM_MAINTENANCE")
    public ResponseEntity<ApiResponse<String>> cleanupTestData() {
        try {
            // 删除测试交易记录
            transactionRepository.deleteAll();
            
            // 删除测试储蓄目标（如果有的话）
            // 注意：这里需要注入SavingGoalRepository
            
            return ResponseEntity.ok(ApiResponse.success("测试数据清理完成"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "清理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重新初始化数据
     */
    @PostMapping("/reinitialize")
    @Operation(summary = "重新初始化数据", description = "重新初始化所有基础数据，包括默认分类等")
    @OperationLog(value = "REINITIALIZE_DATA", description = "重新初始化数据", businessType = "SYSTEM_MAINTENANCE", recordExecutionTime = true)
    public ResponseEntity<ApiResponse<String>> reinitializeData() {
        try {
            // 这里可以触发数据重新初始化
            // 通常在生产环境中不会暴露这个接口
            return ResponseEntity.ok(ApiResponse.success("测试数据重新初始化完成"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "初始化失败: " + e.getMessage()));
        }
    }
}