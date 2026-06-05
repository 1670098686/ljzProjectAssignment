package com.campus.trade.repository.projection;

import com.campus.trade.model.enums.ReportStatus;

public interface ReportStatusCountView {
    ReportStatus getStatus();
    long getTotal();
}
