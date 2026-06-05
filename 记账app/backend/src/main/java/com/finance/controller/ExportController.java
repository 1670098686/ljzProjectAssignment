package com.finance.controller;

import com.finance.dto.ExportRequest;
import com.finance.dto.ExportRequest.ExportFormat;
import com.finance.service.export.ExportFileService;
import com.finance.service.export.ExportedFile;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一的导出接口，提供交易、预算以及汇总导出功能。
 */
@RestController
@RequestMapping("/api/v1/export")
@Validated
public class ExportController {

    private final ExportFileService exportFileService;

    public ExportController(ExportFileService exportFileService) {
        this.exportFileService = exportFileService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<byte[]> exportTransactions(@Valid ExportRequest request) {
        ExportedFile file = exportFileService.exportTransactions(request);
        return buildResponse(file);
    }

    @GetMapping("/budgets")
    public ResponseEntity<byte[]> exportBudgets(@RequestParam("format") ExportFormat format,
                                                @RequestParam(value = "year", required = false) Integer year,
                                                @RequestParam(value = "month", required = false) Integer month) {
        ExportedFile file = exportFileService.exportBudgets(format, year, month);
        return buildResponse(file);
    }

    @GetMapping("/all")
    public ResponseEntity<byte[]> exportAll(@Valid ExportRequest request,
                                            @RequestParam(value = "year", required = false) Integer year,
                                            @RequestParam(value = "month", required = false) Integer month) {
        ExportedFile file = exportFileService.exportAllData(request, year, month);
        return buildResponse(file);
    }

    @GetMapping("/saving-goals")
    public ResponseEntity<byte[]> exportSavingGoals(@RequestParam("format") ExportFormat format) {
        ExportedFile file = exportFileService.exportSavingGoals(format);
        return buildResponse(file);
    }

    @GetMapping("/statistics")
    public ResponseEntity<byte[]> exportStatistics(@RequestParam("format") ExportFormat format,
                                                   @RequestParam(value = "startDate", required = false) String startDate,
                                                   @RequestParam(value = "endDate", required = false) String endDate,
                                                   @RequestParam(value = "granularity", defaultValue = "daily") String granularity) {
        ExportedFile file = exportFileService.exportStatistics(format, startDate, endDate, granularity);
        return buildResponse(file);
    }

    private ResponseEntity<byte[]> buildResponse(ExportedFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.fileName())
                .contentType(file.mediaType())
                .body(file.content());
    }
}
