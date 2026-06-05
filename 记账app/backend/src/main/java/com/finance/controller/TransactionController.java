package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.dto.BatchDeleteRequest;
import com.finance.dto.CreateTransactionRequest;
import com.finance.dto.ExportRequest;
import com.finance.dto.SortDirection;
import com.finance.dto.TransactionDto;
import com.finance.dto.TransactionQuery;
import com.finance.dto.UpdateTransactionRequest;
import com.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "交易记录管理", description = "交易记录的增删改查和查询统计接口，支持多条件筛选和分页查询")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(
        summary = "查询交易记录列表", 
        description = "根据时间范围、交易类型、分类等条件查询交易记录，支持分页和排序。默认查询最近30天的交易记录。",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "成功获取交易记录列表",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "成功示例",
                            value = "{\"code\": 200, \"message\": \"Success\", \"data\": [{\"id\": 1, \"type\": 1, \"categoryId\": 1, \"categoryName\": \"工资\", \"amount\": 5000.0, \"transactionDate\": \"2024-01-15\", \"remark\": \"1月份工资\"}]}"
                        )
                    }
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "请求参数错误，如日期格式不正确"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "未授权，需要登录"
            )
        }
    )
    @OperationLog(value = "QUERY_TRANSACTION", description = "查询交易记录列表", recordParams = false, recordResult = false)
    public ApiResponse<List<TransactionDto>> queryTransactions(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd，示例：2024-01-01") @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd，示例：2024-12-31") @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "交易类型：1=收入，2=支出，示例：1") @RequestParam(value = "type", required = false) Integer type,
            @Parameter(description = "分类ID，示例：1") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "排序方式：asc=升序，desc=降序，默认desc，示例：desc") @RequestParam(value = "sort", defaultValue = "desc") String sort) {

        TransactionQuery query = new TransactionQuery();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setType(type);
        query.setCategoryId(categoryId);
        query.setSortDirection(SortDirection.from(sort));

        return ApiResponse.success(transactionService.queryTransactions(query));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取交易记录详情", 
        description = "根据ID获取单条交易记录的详细信息，包括金额、分类、备注等完整信息",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "成功获取交易记录详情",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "成功示例",
                            value = "{\"code\": 200, \"message\": \"Success\", \"data\": {\"id\": 1, \"type\": 1, \"categoryId\": 1, \"categoryName\": \"工资\", \"amount\": 5000.0, \"transactionDate\": \"2024-01-15\", \"remark\": \"1月份工资\"}}"
                        )
                    }
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "交易记录不存在"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "未授权，需要登录"
            )
        }
    )
    @OperationLog(value = "GET_TRANSACTION", description = "获取交易记录详情", recordParams = false, recordResult = false)
    public ApiResponse<TransactionDto> getTransaction(
            @Parameter(description = "交易记录ID，示例：1") @PathVariable Long id) {
        return ApiResponse.success(transactionService.getTransaction(id));
    }

    @PostMapping
    @Operation(
        summary = "创建交易记录", 
        description = "创建新的交易记录，包含金额、交易类型、分类、日期和备注等信息"
    )
    @OperationLog(value = "CREATE_TRANSACTION", description = "创建交易记录", businessType = "TRANSACTION")
    public ApiResponse<TransactionDto> createTransaction(
            @Parameter(description = "交易记录创建请求体，包含交易详细信息") @Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.success(transactionService.createTransaction(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新交易记录", 
        description = "更新指定ID的交易记录信息，支持修改金额、分类、备注等字段"
    )
    @OperationLog(value = "UPDATE_TRANSACTION", description = "更新交易记录", businessType = "TRANSACTION")
    public ApiResponse<TransactionDto> updateTransaction(
            @Parameter(description = "交易记录ID，示例：1") @PathVariable Long id,
            @Parameter(description = "交易记录更新请求体") @Valid @RequestBody UpdateTransactionRequest request) {
        return ApiResponse.success(transactionService.updateTransaction(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除交易记录", 
        description = "删除指定ID的交易记录，删除后数据不可恢复"
    )
    @OperationLog(value = "DELETE_TRANSACTION", description = "删除交易记录", businessType = "TRANSACTION")
    public ApiResponse<Void> deleteTransaction(
            @Parameter(description = "交易记录ID，示例：1") @PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ApiResponse.successMessage("Transaction deleted");
    }

    @DeleteMapping("/batch")
    @Operation(
        summary = "批量删除交易记录", 
        description = "根据交易记录ID列表批量删除多条交易记录，支持防误操作确认"
    )
    @OperationLog(value = "BATCH_DELETE_TRANSACTION", description = "批量删除交易记录", businessType = "TRANSACTION")
    public ApiResponse<Integer> batchDeleteTransactions(
            @Parameter(description = "批量删除请求体，包含交易记录ID列表和删除确认状态") 
            @Valid @RequestBody BatchDeleteRequest request) {
        int deletedCount = transactionService.batchDeleteTransactions(request);
        return ApiResponse.success(deletedCount, "成功删除" + deletedCount + "条交易记录");
    }

    @PostMapping("/export")
    @Operation(
        summary = "导出交易记录", 
        description = "根据过滤条件导出交易记录为CSV、Excel或JSON格式"
    )
    @OperationLog(value = "EXPORT_TRANSACTION", description = "导出交易记录", businessType = "TRANSACTION", recordParams = true, recordExecutionTime = true)
    public ResponseEntity<byte[]> exportTransactions(
            @Parameter(description = "导出请求体，包含过滤条件和格式设置") 
            @Valid @RequestBody ExportRequest request) {
        byte[] data = transactionService.exportTransactions(request);
        
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(request.getFormat()));
        headers.setContentDispositionFormData("attachment", getFileName(request.getFormat()));
        headers.setContentLength(data.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * 根据导出格式获取媒体类型
     */
    private MediaType getMediaType(ExportRequest.ExportFormat format) {
        switch (format) {
            case CSV:
                return MediaType.TEXT_PLAIN; // CSV使用text/plain
            case EXCEL:
                return MediaType.APPLICATION_OCTET_STREAM; // Excel使用二进制流
            case JSON:
                return MediaType.APPLICATION_JSON;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 生成文件名
     */
    private String getFileName(ExportRequest.ExportFormat format) {
        String timestamp = LocalDate.now().toString();
        switch (format) {
            case CSV:
                return "transactions_" + timestamp + ".csv";
            case EXCEL:
                return "transactions_" + timestamp + ".xlsx";
            case JSON:
                return "transactions_" + timestamp + ".json";
            default:
                return "transactions_" + timestamp + ".txt";
        }
    }
}
