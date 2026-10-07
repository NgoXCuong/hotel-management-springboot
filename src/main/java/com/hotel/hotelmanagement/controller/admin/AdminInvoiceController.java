package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.Invoice;
import com.hotel.hotelmanagement.entity.Payment;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.InvoiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/invoices")
public class AdminInvoiceController {

    private final InvoiceService invoiceService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public AdminInvoiceController(
            InvoiceService invoiceService,
            BookingService bookingService,
            UserRepository userRepository) {
        this.invoiceService = invoiceService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    /**
     * Màn hình danh sách Hóa đơn & Thu ngân (list.html)
     */
    @GetMapping
    public String listInvoices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Invoice.InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<Invoice> invoicesPage = invoiceService.searchInvoices(
                keyword, status, fromDate, toDate,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("invoices", invoicesPage.getContent());
        model.addAttribute("invoicesPage", invoicesPage);
        model.addAttribute("statuses", Invoice.InvoiceStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        // Thống kê KPI
        model.addAttribute("totalInvoices", invoiceService.countTotal());
        model.addAttribute("issuedCount", invoiceService.countIssued());
        model.addAttribute("draftCount", invoiceService.countDraft());
        model.addAttribute("cancelledCount", invoiceService.countCancelled());
        model.addAttribute("totalRevenue", invoiceService.calculateTotalRevenue());

        return "admin/invoice/list";
    }

    /**
     * Tạo hoặc lấy hóa đơn nháp từ đơn đặt phòng
     */
    @GetMapping("/create-from-booking/{bookingId}")
    public String createFromBooking(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "8.00") BigDecimal vatRate,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = null;
            if (authentication != null) {
                currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            }

            Invoice invoice = invoiceService.createOrGetDraftInvoice(bookingId, vatRate, currentUser);
            return "redirect:/admin/invoices/view/" + invoice.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo hóa đơn: " + e.getMessage());
            return "redirect:/admin/bookings/" + bookingId;
        }
    }

    /**
     * Màn hình Xem chi tiết Hóa đơn (view.html)
     */
    @GetMapping("/view/{id}")
    public String viewInvoice(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Invoice> invoiceOpt = invoiceService.findById(id);
        if (invoiceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hóa đơn có ID: " + id);
            return "redirect:/admin/invoices";
        }

        Invoice invoice = invoiceOpt.get();
        model.addAttribute("invoice", invoice);
        model.addAttribute("paymentMethods", Payment.PaymentMethod.values());

        return "admin/invoice/view";
    }

    /**
     * Ghi nhận thanh toán và phát hành hóa đơn
     */
    @PostMapping("/{id}/pay")
    public String payAndIssueInvoice(
            @PathVariable Long id,
            @RequestParam Payment.PaymentMethod paymentMethod,
            @RequestParam BigDecimal paymentAmount,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) String note,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = null;
            if (authentication != null) {
                currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            }

            Invoice invoice = invoiceService.processPaymentAndIssue(id, paymentMethod, paymentAmount, transactionCode, note, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận thanh toán và phát hành Hóa đơn " + invoice.getInvoiceCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý thanh toán: " + e.getMessage());
        }
        return "redirect:/admin/invoices/view/" + id;
    }

    /**
     * Màn hình Mẫu In Hóa đơn thanh toán chuẩn A4 (print.html)
     */
    @GetMapping("/print/{id}")
    public String printInvoice(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Invoice> invoiceOpt = invoiceService.findById(id);
        if (invoiceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hóa đơn có ID: " + id);
            return "redirect:/admin/invoices";
        }

        model.addAttribute("invoice", invoiceOpt.get());
        return "admin/invoice/print";
    }

    /**
     * Hủy hóa đơn
     */
    @PostMapping("/{id}/cancel")
    public String cancelInvoice(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Thu ngân hủy hóa đơn") String reason,
            RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.cancelInvoice(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hóa đơn " + invoice.getInvoiceCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy hóa đơn: " + e.getMessage());
        }
        return "redirect:/admin/invoices/view/" + id;
    }
}
