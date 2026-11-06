package com.finance.service.impl;

import com.finance.context.UserContext;
import com.finance.dto.BatchDeleteRequest;
import com.finance.dto.CreateTransactionRequest;
import com.finance.dto.ExportRequest;
import com.finance.dto.SortDirection;
import com.finance.dto.TransactionDto;
import com.finance.dto.TransactionQuery;
import com.finance.dto.UpdateTransactionRequest;
import com.finance.entity.Category;
import com.finance.entity.TransactionRecord;
import com.finance.event.DomainEventPublisher;
import com.finance.event.TransactionCreatedEvent;
import com.finance.exception.BusinessException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.CategoryRepository;
import com.finance.repository.TransactionRecordRepository;
import com.finance.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final int TYPE_INCOME = 1;
    private static final int TYPE_EXPENSE = 2;

    private final TransactionRecordRepository transactionRecordRepository;
    private final CategoryRepository categoryRepository;
    private final UserContext userContext;
    private final DomainEventPublisher eventPublisher;

    public TransactionServiceImpl(TransactionRecordRepository transactionRecordRepository,
                                  CategoryRepository categoryRepository,
                                  UserContext userContext,
                                  DomainEventPublisher eventPublisher) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.categoryRepository = categoryRepository;
        this.userContext = userContext;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<TransactionDto> queryTransactions(TransactionQuery query) {
        LocalDate start = null;
        LocalDate end = null;
        Integer type = null;
        Long categoryId = null;
        SortDirection sortDirection = null;

        if (query != null) {
            start = query.getStartDate();
            end = query.getEndDate();
            type = query.getType();
            categoryId = query.getCategoryId();
            sortDirection = query.getSortDirection();
        }

        if (start == null && end == null) {
            end = LocalDate.now();
            start = end.minusDays(29);
        }

        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("Start date cannot be after end date");
        }

        if (type != null) {
            validateType(type);
        }

        Long userId = userContext.getCurrentUserId();
        List<TransactionRecord> records = transactionRecordRepository.findByFilter(userId, start, end, type, categoryId);

        SortDirection effectiveSort = sortDirection == null ? SortDirection.DESC : sortDirection;
        Comparator<TransactionRecord> comparator = Comparator.comparing(TransactionRecord::getTransactionDate)
                .thenComparing(TransactionRecord::getId);
        if (effectiveSort == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        records.sort(comparator);

        return records.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public TransactionDto getTransaction(Long id) {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Long userId = userContext.getCurrentUserId();
        TransactionRecord record = transactionRecordRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        return toDto(record);
    }

    @Override
    public TransactionDto createTransaction(CreateTransactionRequest request) {
        Objects.requireNonNull(request, "CreateTransactionRequest must not be null");
        validateType(request.getType());

        Long userId = userContext.getCurrentUserId();
        Category category = getCategoryForUser(request.getCategoryId(), userId);

        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setType(request.getType());
        record.setCategory(category);
        record.setAmount(request.getAmount());
        record.setTransactionDate(request.getTransactionDate());
        record.setRemark(request.getRemark());

        TransactionRecord saved = transactionRecordRepository.save(record);
        publishCreatedEvent(saved);
        return toDto(saved);
    }

    @Override
    public TransactionDto updateTransaction(Long id, UpdateTransactionRequest request) {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Objects.requireNonNull(request, "UpdateTransactionRequest must not be null");
        validateType(request.getType());

        Long userId = userContext.getCurrentUserId();
        TransactionRecord record = transactionRecordRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        Category category = getCategoryForUser(request.getCategoryId(), userId);

        record.setType(request.getType());
        record.setCategory(category);
        record.setAmount(request.getAmount());
        record.setTransactionDate(request.getTransactionDate());
        record.setRemark(request.getRemark());

        return toDto(transactionRecordRepository.save(record));
    }

    @Override
    public void deleteTransaction(Long id) {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Long userId = userContext.getCurrentUserId();
        if (!transactionRecordRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Transaction not found: " + id);
        }
        transactionRecordRepository.deleteById(id);
    }

    @Override
    @Transactional
    public int batchDeleteTransactions(BatchDeleteRequest request) {
        Objects.requireNonNull(request, "BatchDeleteRequest must not be null");
        Objects.requireNonNull(request.getConfirmDelete(), "confirmDelete must not be null");
        
        if (!request.getConfirmDelete()) {
            throw new BusinessException("删除确认状态必须为true");
        }
        
        List<Long> transactionIds = request.getTransactionIds();
        if (transactionIds == null || transactionIds.isEmpty()) {
            throw new BusinessException("交易记录ID列表不能为空");
        }
        
        Long userId = userContext.getCurrentUserId();
        int deletedCount = 0;
        
        // 批量删除事务处理
        for (Long transactionId : transactionIds) {
            if (transactionId != null && transactionRecordRepository.existsByIdAndUserId(transactionId, userId)) {
                transactionRecordRepository.deleteById(transactionId);
                deletedCount++;
            }
        }
        
        return deletedCount;
    }

    @Override
    public byte[] exportTransactions(ExportRequest request) {
        Objects.requireNonNull(request, "ExportRequest must not be null");
        Objects.requireNonNull(request.getFormat(), "Export format must not be null");

        // 解析过滤条件
        Long userId = userContext.getCurrentUserId();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        Integer type = request.getType();
        String category = request.getCategory();

        // 构建查询条件
        List<TransactionRecord> records = transactionRecordRepository.findByFilter(userId, startDate, endDate, type, null);

        // 应用额外过滤条件
        if (type != null) {
            validateType(type);
            records = records.stream().filter(r -> r.getType().equals(type)).collect(Collectors.toList());
        }

        if (category != null && !category.trim().isEmpty()) {
            records = records.stream().filter(r -> r.getCategory().getName().equals(category)).collect(Collectors.toList());
        }

        if (request.getMinAmount() != null) {
            BigDecimal minAmount = request.getMinAmount();
            records = records.stream().filter(r -> r.getAmount().compareTo(minAmount) >= 0).collect(Collectors.toList());
        }

        if (request.getMaxAmount() != null) {
            BigDecimal maxAmount = request.getMaxAmount();
            records = records.stream().filter(r -> r.getAmount().compareTo(maxAmount) <= 0).collect(Collectors.toList());
        }

        if (request.getRemarkKeyword() != null && !request.getRemarkKeyword().trim().isEmpty()) {
            String keyword = request.getRemarkKeyword().toLowerCase();
            records = records.stream().filter(r -> 
                r.getRemark() != null && r.getRemark().toLowerCase().contains(keyword)).collect(Collectors.toList());
        }

        if (request.getIncludeCategories() != null && !request.getIncludeCategories().isEmpty()) {
            records = records.stream().filter(r -> 
                request.getIncludeCategories().contains(r.getCategory().getName())).collect(Collectors.toList());
        }

        if (request.getExcludeCategories() != null && !request.getExcludeCategories().isEmpty()) {
            records = records.stream().filter(r -> 
                !request.getExcludeCategories().contains(r.getCategory().getName())).collect(Collectors.toList());
        }

        // 排序处理
        Comparator<TransactionRecord> comparator = getComparator(request.getSortBy(), request.getSortDirection());
        if (comparator != null) {
            records.sort(comparator);
        }

        // 根据格式生成导出文件
        try {
            switch (request.getFormat()) {
                case CSV:
                    return generateCsv(records);
                case EXCEL:
                    return generateExcel(records);
                case JSON:
                    return generateJson(records);
                default:
                    throw new BusinessException("不支持的导出格式: " + request.getFormat());
            }
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 根据排序字段和方向创建比较器
     */
    private Comparator<TransactionRecord> getComparator(String sortBy, SortDirection sortDirection) {
        if (sortBy == null) {
            return null;
        }

        Comparator<TransactionRecord> comparator = null;
        switch (sortBy.toLowerCase()) {
            case "date":
            case "transactiondate":
                comparator = Comparator.comparing(TransactionRecord::getTransactionDate);
                break;
            case "amount":
                comparator = Comparator.comparing(TransactionRecord::getAmount);
                break;
            case "category":
                comparator = Comparator.comparing(r -> r.getCategory().getName());
                break;
            case "type":
                comparator = Comparator.comparing(TransactionRecord::getType);
                break;
            case "createdat":
                comparator = Comparator.comparing(TransactionRecord::getCreateTime);
                break;
            default:
                // 使用默认排序（按日期降序）
                comparator = Comparator.comparing(TransactionRecord::getTransactionDate).reversed();
                break;
        }

        return (sortDirection == SortDirection.ASC) ? comparator : comparator.reversed();
    }

    /**
     * 生成CSV格式文件
     */
    private byte[] generateCsv(List<TransactionRecord> records) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // CSV头部
        csv.append("ID,交易类型,分类,金额,交易日期,备注,创建时间\\n");
        
        // 数据行
        for (TransactionRecord record : records) {
            csv.append(record.getId()).append(",");
            csv.append(record.getType() == TYPE_INCOME ? "收入" : "支出").append(",");
            csv.append(record.getCategory().getName()).append(",");
            csv.append(record.getAmount()).append(",");
            csv.append(record.getTransactionDate().format(dateFormatter)).append(",");
            csv.append(record.getRemark() != null ? escapeCsvField(record.getRemark()) : "").append(",");
            csv.append(record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 转义CSV字段中的特殊字符
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果字段包含逗号、双引号或换行符，需要用双引号包围并转义双引号
        if (field.contains(",") || field.contains("\"") || field.contains("\\n") || field.contains("\\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * 生成Excel格式文件（使用Apache POI生成真正的Excel文件）
     */
    private byte[] generateExcel(List<TransactionRecord> records) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建工作表
            Sheet sheet = workbook.createSheet("交易记录");
            
            // 设置列宽
            sheet.setColumnWidth(0, 15); // ID
            sheet.setColumnWidth(1, 20); // 交易类型
            sheet.setColumnWidth(2, 20); // 分类
            sheet.setColumnWidth(3, 15); // 金额
            sheet.setColumnWidth(4, 20); // 交易日期
            sheet.setColumnWidth(5, 30); // 备注
            sheet.setColumnWidth(6, 20); // 创建时间
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "交易类型", "分类", "金额", "交易日期", "备注", "创建时间"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 格式化日期
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            // 填充数据行
            for (int rowIndex = 0; rowIndex < records.size(); rowIndex++) {
                TransactionRecord record = records.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                
                // ID
                row.createCell(0).setCellValue(record.getId());
                
                // 交易类型
                row.createCell(1).setCellValue(record.getType() == TYPE_INCOME ? "收入" : "支出");
                
                // 分类
                row.createCell(2).setCellValue(record.getCategory().getName());
                
                // 金额（格式化为货币格式）
                Cell amountCell = row.createCell(3);
                amountCell.setCellValue(record.getAmount().doubleValue());
                CellStyle currencyStyle = workbook.createCellStyle();
                currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
                amountCell.setCellStyle(currencyStyle);
                
                // 交易日期
                row.createCell(4).setCellValue(record.getTransactionDate().format(dateFormatter));
                
                // 备注
                String remark = record.getRemark() != null ? record.getRemark() : "";
                row.createCell(5).setCellValue(remark);
                
                // 创建时间
                row.createCell(6).setCellValue(record.getCreateTime().format(dateTimeFormatter));
            }
            
            // 添加汇总行
            int summaryRow = records.size() + 2;
            Row totalRow = sheet.createRow(summaryRow);
            
            // 合并收入单元格
            totalRow.createCell(0).setCellValue("总计：");
            Cell totalAmountCell = totalRow.createCell(1);
            
            // 计算总收入和总支出
            BigDecimal totalIncome = records.stream()
                .filter(r -> r.getType() == TYPE_INCOME)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalExpense = records.stream()
                .filter(r -> r.getType() == TYPE_EXPENSE)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 设置汇总信息
            totalAmountCell.setCellValue(String.format("收入: %.2f, 支出: %.2f, 余额: %.2f", 
                totalIncome.doubleValue(), totalExpense.doubleValue(), 
                totalIncome.subtract(totalExpense).doubleValue()));
            
            // 设置汇总行样式
            CellStyle summaryStyle = workbook.createCellStyle();
            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryStyle.setFont(summaryFont);
            summaryStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            totalRow.getCell(1).setCellStyle(summaryStyle);
            
            // 添加导出时间
            summaryRow++;
            Row exportTimeRow = sheet.createRow(summaryRow);
            Cell exportTimeCell = exportTimeRow.createCell(1);
            exportTimeCell.setCellValue("导出时间：" + LocalDateTime.now().format(dateTimeFormatter));
            CellStyle timeStyle = workbook.createCellStyle();
            timeStyle.setFont(summaryFont);
            exportTimeCell.setCellStyle(timeStyle);
            
            // 输出到字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            return outputStream.toByteArray();
        }
    }

    /**
     * 生成JSON格式文件
     */
    private byte[] generateJson(List<TransactionRecord> records) {
        StringBuilder json = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        json.append("{\\n  \"transactions\": [\\n");
        
        for (int i = 0; i < records.size(); i++) {
            TransactionRecord record = records.get(i);
            json.append("    {\\n");
            json.append("      \"id\": ").append(record.getId()).append(",\\n");
            json.append("      \"type\": ").append(record.getType()).append(",\\n");
            json.append("      \"typeName\": \"").append(record.getType() == TYPE_INCOME ? "收入" : "支出").append("\",\\n");
            json.append("      \"category\": \"").append(record.getCategory().getName()).append("\",\\n");
            json.append("      \"amount\": ").append(record.getAmount()).append(",\\n");
            json.append("      \"transactionDate\": \"").append(record.getTransactionDate().format(dateFormatter)).append("\",\\n");
            json.append("      \"remark\": ").append(record.getRemark() != null ? "\"" + escapeJsonString(record.getRemark()) + "\"" : "null").append(",\\n");
            json.append("      \"createdAt\": \"").append(record.getCreateTime().format(dateFormatter)).append("\"\\n");
            json.append("    }");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\\n");
        }
        
        json.append("  ],\\n");
        json.append("  \"total\": ").append(records.size()).append(",\\n");
        json.append("  \"exportedAt\": \"").append(LocalDate.now().format(dateFormatter)).append("\"\\n");
        json.append("}\\n");
        
        return json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\"", "\\\"")
                  .replace("\\", "\\\\")
                  .replace("\\n", "\\\\n")
                  .replace("\\r", "\\\\r")
                  .replace("\\t", "\\\\t");
    }

    private void validateType(Integer type) {
        if (type == null || (type != TYPE_INCOME && type != TYPE_EXPENSE)) {
            throw new BusinessException("Transaction type must be 1 (income) or 2 (expense)");
        }
    }

    private TransactionDto toDto(TransactionRecord record) {
        TransactionDto dto = new TransactionDto();
        dto.setId(record.getId());
        dto.setType(record.getType());
        dto.setCategoryId(record.getCategory().getId());
        dto.setCategoryName(record.getCategory().getName());
        dto.setAmount(record.getAmount());
        dto.setTransactionDate(record.getTransactionDate());
        dto.setRemark(record.getRemark());
        return dto;
    }

    private Category getCategoryForUser(Long categoryId, Long userId) {
        if (categoryId == null) {
            throw new BusinessException("Category id is required");
        }
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private void publishCreatedEvent(TransactionRecord record) {
        if (record == null) {
            return;
        }
        eventPublisher.publish(new TransactionCreatedEvent(
                record.getUserId(),
                record.getId(),
                record.getType(),
                record.getCategory().getId(),
                record.getAmount(),
                record.getTransactionDate()));
    }
}
