package com.campus.trade.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {
    @Schema(description = "Number of successful items")
    private long successCount;

    @Schema(description = "Number of failed items")
    private long failedCount;

    @Schema(description = "Total items processed")
    private long totalCount;

    @Schema(description = "Optional batch level message")
    private String message;
}
