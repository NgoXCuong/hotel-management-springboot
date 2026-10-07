package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.dto.DashboardSummaryDTO;

public interface DashboardService {

    /**
     * Tổng hợp các chỉ số KPI, biểu đồ và đơn đặt mới nhất cho Dashboard
     */
    DashboardSummaryDTO getDashboardSummary();

    /**
     * Lấy dữ liệu biểu đồ doanh thu theo mốc thời gian tùy chọn (7 ngày, 14 ngày, 30 ngày, tháng này, tháng trước)
     */
    java.util.Map<String, Object> getRevenueChartData(String period);
}
