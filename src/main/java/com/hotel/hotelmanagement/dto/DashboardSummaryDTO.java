package com.hotel.hotelmanagement.dto;

import com.hotel.hotelmanagement.entity.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO tổng hợp dữ liệu thời gian thực cho Dashboard Tổng quan.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {

    // 4 Thẻ KPI hàng đầu
    private BigDecimal todayRevenue;
    private double revenueGrowthPercent; // Tỷ lệ % tăng/giảm so với hôm qua
    private boolean growthPositive;

    private int occupancyRate; // Công suất phòng (%)
    private long occupiedRooms;
    private long totalRooms;

    private long todayCheckIns; // Lượt check-in hôm nay
    private long pendingBookings; // Số đơn PENDING cần duyệt

    // Biểu đồ 7 ngày gần nhất
    private List<String> revenueChartLabels; // ["17/08", "18/08", "19/08", "20/08", "21/08", "22/08", "23/08"]
    private List<BigDecimal> revenueChartData;

    // Biểu đồ Hiện trạng phòng (Doughnut Chart)
    private long availableRooms;
    private long cleaningRooms;
    private long maintenanceRooms;

    // Phân bổ nguồn đặt (Booking Sources)
    private long walkInCount;
    private long phoneCount;
    private long onlineCount;
    private long otaCount;

    // Top 5 Loại phòng doanh thu cao nhất
    private List<RoomTypeRevenueDTO> topRoomTypes;

    // 5 Đơn đặt phòng mới nhất
    private List<Booking> recentBookings;
}
