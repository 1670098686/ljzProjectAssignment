package com.finance.service.export;

import com.finance.dto.BudgetDto;
import com.finance.dto.ExportRequest;
import com.finance.dto.ExportRequest.ExportFormat;
import com.finance.dto.SavingGoalDto;
import com.finance.dto.StatisticsGranularity;
import com.finance.exception.BusinessException;
import com.finance.service.BudgetService;
import com.finance.service.SavingGoalService;
import com.finance.service.StatisticsService;
import com.finance.service.TransactionService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * 负责统一处理后端导出文件生成与命名逻辑，供 REST 控制器直接复用。
 */
@Service
public class ExportFileService {

    private static final MediaType MEDIA_CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");
    private static final MediaType MEDIA_EXCEL = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType MEDIA_ZIP = MediaType.parseMediaType("application/zip");

    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final SavingGoalService savingGoalService;
    private final StatisticsService statisticsService;

    public ExportFileService(TransactionService transactionService, BudgetService budgetService, 
                             SavingGoalService savingGoalService, StatisticsService statisticsService) {
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.savingGoalService = savingGoalService;
        this.statisticsService = statisticsService;
    }

    public ExportedFile exportTransactions(ExportRequest request) {
        byte[] bytes = transactionService.exportTransactions(request);
        String fileName = buildFileName("transactions", request.getFormat());
        return new ExportedFile(bytes, fileName, resolveMediaType(request.getFormat()));
    }

    public ExportedFile exportBudgets(ExportFormat format, Integer year, Integer month) {
        validateYearMonthPair(year, month);
        List<BudgetDto> budgets = budgetService.listBudgets(year, month);
        byte[] bytes = switch (format) {
            case CSV -> buildBudgetCsv(budgets);
            case EXCEL -> buildBudgetExcel(budgets);
            case JSON -> throw new BusinessException("预算导出暂不支持 JSON 格式");
        };
        String fileName = buildFileName("budgets", format);
        return new ExportedFile(bytes, fileName, resolveMediaType(format));
    }

    public ExportedFile exportAllData(ExportRequest request, Integer year, Integer month) {
        validateYearMonthPair(year, month);
        ExportedFile transactions = exportTransactions(request);
        ExportedFile budgets = exportBudgets(request.getFormat(), year, month);

        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put(transactions.fileName(), transactions.content());
        files.put(budgets.fileName(), budgets.content());

        byte[] zipped = zipFiles(files);
        String fileName = "finance_all_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ".zip";
        return new ExportedFile(zipped, fileName, MEDIA_ZIP);
    }

    private byte[] zipFiles(Map<String, byte[]> files) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(baos)) {

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("压缩导出文件失败: " + e.getMessage());
        }
    }

    private byte[] buildBudgetCsv(List<BudgetDto> budgets) {
        StringBuilder builder = new StringBuilder();
        builder.append("ID,分类,预算金额,年份,月份\n");
        for (BudgetDto budget : budgets) {
            builder.append(budget.getId()).append(',');
            builder.append(escapeCsvField(budget.getCategoryName())).append(',');
            builder.append(budget.getAmount()).append(',');
            builder.append(budget.getYear() == null ? "" : budget.getYear()).append(',');
            builder.append(budget.getMonth() == null ? "" : budget.getMonth()).append('\n');
        }
        return builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] buildBudgetExcel(List<BudgetDto> budgets) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("预算数据");
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 12 * 256);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "分类", "预算金额", "年份", "月份"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < budgets.size(); i++) {
                BudgetDto budget = budgets.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(budget.getId() == null ? 0 : budget.getId());
                row.createCell(1).setCellValue(budget.getCategoryName());
                Cell amountCell = row.createCell(2);
                amountCell.setCellValue(budget.getAmount() == null ? 0 : budget.getAmount().doubleValue());
                row.createCell(3).setCellValue(budget.getYear() == null ? 0 : budget.getYear().doubleValue());
                row.createCell(4).setCellValue(budget.getMonth() == null ? 0 : budget.getMonth().doubleValue());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成预算 Excel 失败: " + e.getMessage());
        }
    }

    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String buildFileName(String prefix, ExportFormat format) {
        String extension = switch (format) {
            case CSV -> "csv";
            case EXCEL -> "xlsx";
            case JSON -> "json";
        };
        return prefix + "_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "." + extension;
    }

    private MediaType resolveMediaType(ExportFormat format) {
        return switch (format) {
            case CSV -> MEDIA_CSV;
            case EXCEL -> MEDIA_EXCEL;
            case JSON -> MediaType.APPLICATION_JSON;
        };
    }

    private void validateYearMonthPair(Integer year, Integer month) {
        if ((year == null && month == null) || (year != null && month != null)) {
            return;
        }
        throw new BusinessException("年份与月份需要同时提供，或同时留空");
    }
    
    /**
     * 验证CSV文件内容
     * @param content CSV文件内容
     * @param expectedHeader 预期的CSV文件头部
     */
    private void validateCsvContent(byte[] content, String expectedHeader) {
        String csvContent = new String(content, java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\\n");
        if (lines.length == 0) {
            throw new BusinessException("CSV文件内容为空");
        }
        if (!lines[0].equals(expectedHeader)) {
            throw new BusinessException("CSV文件头部不符合预期");
        }
    }

    public ExportedFile exportSavingGoals(ExportFormat format) {
        List<SavingGoalDto> savingGoals = savingGoalService.listSavingGoals();
        byte[] bytes = switch (format) {
            case CSV -> buildSavingGoalsCsv(savingGoals);
            case EXCEL -> buildSavingGoalsExcel(savingGoals);
            case JSON -> throw new BusinessException("储蓄目标导出暂不支持 JSON 格式");
        };
        String fileName = buildFileName("saving_goals", format);
        return new ExportedFile(bytes, fileName, resolveMediaType(format));
    }

    public ExportedFile exportStatistics(ExportFormat format, String startDate, String endDate, String granularity) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        // 这里需要根据granularity参数调用不同的统计方法
        // 由于StatisticsService的getTrend方法需要StatisticsGranularity枚举，我们需要转换
        StatisticsGranularity statsGranularity = StatisticsGranularity.from(granularity);
        
        List<com.finance.dto.TrendPointDto> trendData = statisticsService.getTrend(start, end, statsGranularity);
        
        byte[] bytes = switch (format) {
            case CSV -> buildStatisticsCsv(trendData);
            case EXCEL -> buildStatisticsExcel(trendData);
            case JSON -> throw new BusinessException("统计报表导出暂不支持 JSON 格式");
        };
        String fileName = buildFileName("statistics", format);
        return new ExportedFile(bytes, fileName, resolveMediaType(format));
    }

    private byte[] buildSavingGoalsCsv(List<SavingGoalDto> savingGoals) {
        StringBuilder builder = new StringBuilder();
        builder.append("ID,目标名称,目标金额,当前金额,截止日期,描述,进度\n");
        for (SavingGoalDto goal : savingGoals) {
            builder.append(goal.getId()).append(',');
            builder.append(escapeCsvField(goal.getName())).append(',');
            builder.append(goal.getTargetAmount()).append(',');
            builder.append(goal.getCurrentAmount()).append(',');
            builder.append(goal.getDeadline()).append(',');
            builder.append(escapeCsvField(goal.getDescription())).append(',');
            builder.append(String.format("%.2f%%", (goal.getProgressPercentage() != null ? goal.getProgressPercentage() : BigDecimal.ZERO).doubleValue())).append('\n');
        }
        return builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] buildSavingGoalsExcel(List<SavingGoalDto> savingGoals) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("储蓄目标数据");
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 25 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 16 * 256);
            sheet.setColumnWidth(4, 16 * 256);
            sheet.setColumnWidth(5, 30 * 256);
            sheet.setColumnWidth(6, 12 * 256);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "目标名称", "目标金额", "当前金额", "截止日期", "描述", "进度"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < savingGoals.size(); i++) {
            SavingGoalDto goal = savingGoals.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(goal.getId() == null ? 0 : goal.getId());
            row.createCell(1).setCellValue(goal.getName());
            row.createCell(2).setCellValue(goal.getTargetAmount() == null ? 0 : goal.getTargetAmount().doubleValue());
            row.createCell(3).setCellValue(goal.getCurrentAmount() == null ? 0 : goal.getCurrentAmount().doubleValue());
            row.createCell(4).setCellValue(goal.getDeadline().toString());
            row.createCell(5).setCellValue(goal.getDescription());
            row.createCell(6).setCellValue(String.format("%.2f%%", (goal.getProgressPercentage() != null ? goal.getProgressPercentage() : BigDecimal.ZERO).doubleValue()));
        }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成储蓄目标 Excel 失败: " + e.getMessage());
        }
    }

    private byte[] buildStatisticsCsv(List<com.finance.dto.TrendPointDto> trendData) {
        StringBuilder builder = new StringBuilder();
        builder.append("日期,收入,支出\n");
        for (com.finance.dto.TrendPointDto point : trendData) {
            builder.append(point.getDate()).append(',');
            builder.append(point.getIncome()).append(',');
            builder.append(point.getExpense()).append('\n');
        }
        return builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] buildStatisticsExcel(List<com.finance.dto.TrendPointDto> trendData) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("统计报表数据");
            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 16 * 256);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"日期", "收入", "支出"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < trendData.size(); i++) {
                com.finance.dto.TrendPointDto point = trendData.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(point.getDate().toString());
                row.createCell(1).setCellValue(point.getIncome() == null ? 0 : point.getIncome().doubleValue());
                row.createCell(2).setCellValue(point.getExpense() == null ? 0 : point.getExpense().doubleValue());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成统计报表 Excel 失败: " + e.getMessage());
        }
    }
}
