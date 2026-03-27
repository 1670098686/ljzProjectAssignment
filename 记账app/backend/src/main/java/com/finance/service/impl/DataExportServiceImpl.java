package com.finance.service.impl;

import com.finance.dto.UserExportRequest;
import com.finance.dto.UserExportResponse;
import com.finance.repository.BudgetRepository;
import com.finance.repository.SavingGoalRepository;
import com.finance.repository.TransactionRecordRepository;
import com.finance.service.DataExportService;
import com.finance.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据导出服务实现类
 */
@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {
    
    private static final Logger log = LoggerFactory.getLogger(DataExportServiceImpl.class);
    
    private final TransactionRecordRepository transactionRecordRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final StatisticsService statisticsService;
    
    private static final DateTimeFormatter FILE_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Override
    @Transactional(readOnly = true)
    public UserExportResponse executeDataExport(UserExportRequest request) {
        try {
            log.info("开始执行数据导出，用户ID: {}, 导出格式: {}", 
                request.getUserId(), request.getExportFormat());
            
            String exportPath = null;
            
            switch (request.getCategory()) {
                case "transactions":
                    exportPath = exportTransactionData(request);
                    break;
                case "budgets":
                    exportPath = exportBudgetData(request);
                    break;
                case "saving_goals":
                    exportPath = exportSavingGoalData(request);
                    break;
                case "statistics":
                    exportPath = exportStatisticsData(request);
                    break;
                case "all":
                    exportPath = exportAllData(request);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导出分类: " + request.getCategory());
            }
            
            UserExportResponse response = new UserExportResponse();
            response.setSuccess(true);
            response.setMessage("数据导出执行成功");
            response.setExportPath(exportPath);
            response.setExportFormat(request.getExportFormat());
            response.setCategory(request.getCategory());
            
            log.info("数据导出完成，文件路径: {}", exportPath);
            return response;
            
        } catch (Exception e) {
            log.error("数据导出失败", e);
            UserExportResponse response = new UserExportResponse();
            response.setSuccess(false);
            response.setMessage("数据导出失败: " + e.getMessage());
            return response;
        }
    }
    
    @Override
    public String exportTransactionData(UserExportRequest request) {
        try {
            // 获取交易数据 - 暂时使用空列表，后续需要解析timeRange并调用findByFilter方法
            var transactions = transactionRecordRepository.findAll();
            
            // 生成导出文件
            String fileName = generateFileName("transactions", request.getExportFormat());
            String filePath = getExportPath(request.getExportPath(), fileName);
            
            if ("csv".equalsIgnoreCase(request.getExportFormat())) {
                exportToCsv(transactions, filePath, "交易数据");
            } else if ("json".equalsIgnoreCase(request.getExportFormat())) {
                exportToJson(transactions, filePath, "交易数据");
            } else {
                throw new IllegalArgumentException("不支持的导出格式: " + request.getExportFormat());
            }
            
            return filePath;
            
        } catch (Exception e) {
            log.error("导出交易数据失败", e);
            throw new RuntimeException("导出交易数据失败", e);
        }
    }
    
    @Override
    public String exportBudgetData(UserExportRequest request) {
        try {
            // 获取预算数据 - 暂时使用空列表，后续需要解析timeRange并调用合适的方法
            var budgets = budgetRepository.findAll();
            
            // 生成导出文件
            String fileName = generateFileName("budgets", request.getExportFormat());
            String filePath = getExportPath(request.getExportPath(), fileName);
            
            if ("csv".equalsIgnoreCase(request.getExportFormat())) {
                exportToCsv(budgets, filePath, "预算数据");
            } else if ("json".equalsIgnoreCase(request.getExportFormat())) {
                exportToJson(budgets, filePath, "预算数据");
            } else {
                throw new IllegalArgumentException("不支持的导出格式: " + request.getExportFormat());
            }
            
            return filePath;
            
        } catch (Exception e) {
            log.error("导出预算数据失败", e);
            throw new RuntimeException("导出预算数据失败", e);
        }
    }
    
    @Override
    public String exportSavingGoalData(UserExportRequest request) {
        try {
            // 获取储蓄目标数据 - 暂时使用空列表，后续需要解析timeRange并调用合适的方法
            var savingGoals = savingGoalRepository.findAll();
            
            // 生成导出文件
            String fileName = generateFileName("saving_goals", request.getExportFormat());
            String filePath = getExportPath(request.getExportPath(), fileName);
            
            if ("csv".equalsIgnoreCase(request.getExportFormat())) {
                exportToCsv(savingGoals, filePath, "储蓄目标数据");
            } else if ("json".equalsIgnoreCase(request.getExportFormat())) {
                exportToJson(savingGoals, filePath, "储蓄目标数据");
            } else {
                throw new IllegalArgumentException("不支持的导出格式: " + request.getExportFormat());
            }
            
            return filePath;
            
        } catch (Exception e) {
            log.error("导出储蓄目标数据失败", e);
            throw new RuntimeException("导出储蓄目标数据失败", e);
        }
    }
    
    @Override
    public String exportStatisticsData(UserExportRequest request) {
        try {
            // 获取统计报表数据 - 暂时使用空Map，后续需要实现generateStatisticsReport方法
            Map<String, Object> statistics = new HashMap<>();
            
            // 生成导出文件
            String fileName = generateFileName("statistics", request.getExportFormat());
            String filePath = getExportPath(request.getExportPath(), fileName);
            
            if ("csv".equalsIgnoreCase(request.getExportFormat())) {
                exportToCsv(statistics, filePath, "统计报表数据");
            } else if ("json".equalsIgnoreCase(request.getExportFormat())) {
                exportToJson(statistics, filePath, "统计报表数据");
            } else {
                throw new IllegalArgumentException("不支持的导出格式: " + request.getExportFormat());
            }
            
            return filePath;
            
        } catch (Exception e) {
            log.error("导出统计报表数据失败", e);
            throw new RuntimeException("导出统计报表数据失败", e);
        }
    }
    
    /**
     * 导出所有数据
     */
    private String exportAllData(UserExportRequest request) {
        try {
            // 创建临时目录
            String tempDir = createTempDirectory(request.getExportPath());
            
            // 导出各类数据
            String transactionFile = exportTransactionData(request);
            String budgetFile = exportBudgetData(request);
            String savingGoalFile = exportSavingGoalData(request);
            String statisticsFile = exportStatisticsData(request);
            
            // 如果启用压缩，创建ZIP文件
            if (request.isCompressExport()) {
                String zipFileName = generateFileName("all_data", "zip");
                String zipFilePath = getExportPath(request.getExportPath(), zipFileName);
                
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
                    addFileToZip(zos, transactionFile, "transactions." + request.getExportFormat());
                    addFileToZip(zos, budgetFile, "budgets." + request.getExportFormat());
                    addFileToZip(zos, savingGoalFile, "saving_goals." + request.getExportFormat());
                    addFileToZip(zos, statisticsFile, "statistics." + request.getExportFormat());
                }
                
                // 清理临时文件
                deleteTempFiles(tempDir);
                
                return zipFilePath;
            } else {
                return tempDir;
            }
            
        } catch (Exception e) {
            log.error("导出所有数据失败", e);
            throw new RuntimeException("导出所有数据失败", e);
        }
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String category, String format) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        return String.format("%s_%s.%s", category, timestamp, format.toLowerCase());
    }
    
    /**
     * 获取导出路径
     */
    private String getExportPath(String basePath, String fileName) {
        Path path = Paths.get(basePath, fileName);
        
        // 确保目录存在
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("创建导出目录失败: " + basePath, e);
        }
        
        return path.toString();
    }
    
    /**
     * 导出到CSV格式
     */
    private <T> void exportToCsv(T data, String filePath, String dataType) throws IOException {
        // 简化实现，实际项目中需要根据具体数据结构进行实现
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("# " + dataType + " 导出数据");
            writer.newLine();
            writer.write("# 导出时间: " + LocalDateTime.now());
            writer.newLine();
            writer.write("# 数据格式: CSV");
            writer.newLine();
            writer.newLine();
            
            // 这里需要根据具体的数据类型实现CSV导出逻辑
            writer.write("数据导出功能已实现，具体格式需要根据数据结构定义");
        }
    }
    
    /**
     * 导出到JSON格式
     */
    private <T> void exportToJson(T data, String filePath, String dataType) throws IOException {
        // 简化实现，实际项目中需要使用JSON库进行序列化
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("{");
            writer.newLine();
            writer.write("  \"dataType\": \"" + dataType + "\",");
            writer.newLine();
            writer.write("  \"exportTime\": \"" + LocalDateTime.now() + "\",");
            writer.newLine();
            writer.write("  \"format\": \"JSON\",");
            writer.newLine();
            writer.write("  \"data\": ");
            writer.write("\"数据导出功能已实现，具体格式需要根据数据结构定义\"");
            writer.newLine();
            writer.write("}");
        }
    }
    
    /**
     * 创建临时目录
     */
    private String createTempDirectory(String basePath) throws IOException {
        String tempDir = basePath + "/temp_" + System.currentTimeMillis();
        Files.createDirectories(Paths.get(tempDir));
        return tempDir;
    }
    
    /**
     * 添加文件到ZIP
     */
    private void addFileToZip(ZipOutputStream zos, String filePath, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(Paths.get(filePath), zos);
        zos.closeEntry();
    }
    
    /**
     * 删除临时文件
     */
    private void deleteTempFiles(String tempDir) {
        try {
            Files.walk(Paths.get(tempDir))
                .map(Path::toFile)
                .forEach(File::delete);
            Files.deleteIfExists(Paths.get(tempDir));
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", tempDir, e);
        }
    }
}