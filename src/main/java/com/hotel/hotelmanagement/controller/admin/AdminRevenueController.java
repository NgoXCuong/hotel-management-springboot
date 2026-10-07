package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.dto.RevenueReportDTO;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.service.RevenueService;
import com.hotel.hotelmanagement.service.RoomTypeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/revenue")
public class AdminRevenueController {

    private final RevenueService revenueService;
    private final RoomTypeService roomTypeService;

    public AdminRevenueController(RevenueService revenueService, RoomTypeService roomTypeService) {
        this.revenueService = revenueService;
        this.roomTypeService = roomTypeService;
    }

    /**
     * Màn hình Báo cáo Doanh thu chi tiết (revenue.html)
     */
    @GetMapping
    public String showRevenueReport(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Booking.BookingSource source,
            Model model) {

        RevenueReportDTO report = revenueService.getRevenueReport(period, fromDate, toDate, roomTypeId, source);

        model.addAttribute("report", report);
        model.addAttribute("roomTypes", roomTypeService.findAll());
        model.addAttribute("sources", Booking.BookingSource.values());
        model.addAttribute("period", period);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("source", source);

        return "admin/revenue/index";
    }

    /**
     * Xuất báo cáo Doanh thu ra file CSV / Excel UTF-8
     */
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Booking.BookingSource source) {

        byte[] csvData = revenueService.exportRevenueReportToCsv(period, fromDate, toDate, roomTypeId, source);
        String filename = "bao_cao_doanh_thu_" + period + "_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
