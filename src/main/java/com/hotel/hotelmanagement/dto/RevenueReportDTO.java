package com.hotel.hotelmanagement.dto;

import com.hotel.hotelmanagement.entity.Invoice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO tổng hợp dữ liệu Báo cáo Doanh thu chuyên sâu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportDTO {

    // Khoảng thời gian
    private String period; // today, week, month, year, custom
    private LocalDate fromDate;
    private LocalDate toDate;

    // 4 Chỉ số tài chính tổng hợp
    private BigDecimal totalRevenue; // Tổng doanh thu thực nhận
    private BigDecimal totalRoomRevenue; // Tổng tiền phòng
    private BigDecimal totalServiceRevenue; // Tổng tiền dịch vụ phát sinh
    private BigDecimal totalDiscount; // Tổng chiết khấu voucher
    private BigDecimal totalTax; // Tổng thuế VAT

    // Biểu đồ so sánh 12 tháng (Tiền phòng vs Tiền dịch vụ)
    private List<String> monthlyLabels; // ["T1", "T2", ... "T12"]
    private List<BigDecimal> monthlyRoomRevenue;
    private List<BigDecimal> monthlyServiceRevenue;

    // Biểu đồ tròn cơ cấu doanh thu theo Loại phòng
    private List<String> roomTypeLabels;
    private List<BigDecimal> roomTypeValues;

    // Danh sách chi tiết Hóa đơn đối soát trong kỳ
    private List<Invoice> invoices;
}
