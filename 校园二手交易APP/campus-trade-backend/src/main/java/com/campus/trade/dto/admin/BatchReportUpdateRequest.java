package com.campus.trade.dto.admin;

import com.campus.trade.model.enums.ReportStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BatchReportUpdateRequest {

    @NotEmpty
    @Size(max = 200)
    private List<Long> reportIds;

    @NotNull
    private ReportStatus status;

    @Size(max = 200)
    private String remark;

    public List<Long> getReportIds() {
        return reportIds;
    }

    public void setReportIds(List<Long> reportIds) {
        this.reportIds = reportIds;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
