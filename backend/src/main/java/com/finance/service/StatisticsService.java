package com.finance.service;

import com.finance.dto.CategoryStatisticsDto;
import com.finance.dto.StatisticsGranularity;
import com.finance.dto.SummaryStatisticsDto;
import com.finance.dto.TrendPointDto;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    SummaryStatisticsDto getMonthlySummary(int year, int month);

    List<CategoryStatisticsDto> getCategoryStatistics(LocalDate startDate, LocalDate endDate, Integer type);

    List<TrendPointDto> getTrend(LocalDate startDate, LocalDate endDate, StatisticsGranularity granularity);
}
