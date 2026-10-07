package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.dto.RevenueReportDTO;
import com.hotel.hotelmanagement.entity.Booking;

import java.time.LocalDate;

public interface RevenueService {

    /**
     * Lấy dữ liệu báo cáo tài chính & doanh thu theo khoảng thời gian và bộ lọc
     */
    RevenueReportDTO getRevenueReport(String period, LocalDate fromDate, LocalDate toDate, Long roomTypeId, Booking.BookingSource source);

    /**
     * Xuất dữ liệu báo cáo doanh thu ra file CSV / Excel UTF-8
     */
    byte[] exportRevenueReportToCsv(String period, LocalDate fromDate, LocalDate toDate, Long roomTypeId, Booking.BookingSource source);
}
