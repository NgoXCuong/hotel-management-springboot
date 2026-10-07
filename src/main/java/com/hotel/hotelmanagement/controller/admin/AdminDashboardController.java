package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.dto.DashboardSummaryDTO;
import com.hotel.hotelmanagement.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();
        model.addAttribute("summary", summary);
        return "admin/dashboard";
    }

    /**
     * API tải dữ liệu biểu đồ doanh thu theo các mốc thời gian tùy chọn (7, 14, 30 ngày, tháng này, tháng trước)
     */
    @GetMapping("/admin/api/dashboard/revenue-chart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRevenueChart(@RequestParam(defaultValue = "7") String period) {
        return ResponseEntity.ok(dashboardService.getRevenueChartData(period));
    }
}
