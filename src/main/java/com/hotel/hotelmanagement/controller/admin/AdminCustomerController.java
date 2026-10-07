package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final BookingRepository bookingRepository;

    public AdminCustomerController(CustomerService customerService, BookingRepository bookingRepository) {
        this.customerService = customerService;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Danh sách khách hàng với bộ lọc tìm kiếm nâng cao và thống kê nhanh.
     */
    @GetMapping
    public String listCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Customer.Gender gender,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        Boolean active = null;
        if ("active".equalsIgnoreCase(status)) {
            active = true;
        } else if ("locked".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status)) {
            active = false;
        }

        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<Customer> customersPage = customerService.searchCustomers(
                keyword, gender, nationality, active,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("customers", customersPage.getContent());
        model.addAttribute("customersPage", customersPage);
        model.addAttribute("genders", Customer.Gender.values());
        model.addAttribute("nationalities", customerService.findDistinctNationalities());

        // Thống kê nhanh KPI
        model.addAttribute("totalCount", customerService.countTotal());
        model.addAttribute("activeCount", customerService.countActive());
        model.addAttribute("accountCount", customerService.countWithAccount());

        // Giữ lại trạng thái bộ lọc
        model.addAttribute("paramKeyword", keyword);
        model.addAttribute("paramGender", gender);
        model.addAttribute("paramNationality", nationality);
        model.addAttribute("paramStatus", status);

        return "admin/customer/list";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectCustomerForm() {
        return "redirect:/admin/customers";
    }

    /**
     * Xử lý lưu hồ sơ khách hàng từ Modal (áp dụng cho cả Tạo mới và Cập nhật).
     */
    @PostMapping("/save")
    public String saveCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        boolean isNew = (customer.getId() == null);

        // Kiểm tra trùng lặp Số điện thoại
        if (StringUtils.hasText(customer.getPhone())) {
            if (isNew && customerService.existsByPhone(customer.getPhone())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại [" + customer.getPhone() + "] đã được đăng ký cho khách hàng khác!");
                return "redirect:/admin/customers";
            } else if (!isNew && customerService.existsByPhoneAndIdNot(customer.getPhone(), customer.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại [" + customer.getPhone() + "] đã thuộc về khách hàng khác!");
                return "redirect:/admin/customers";
            }
        }

        // Kiểm tra trùng lặp Số CCCD / Hộ chiếu (nếu có nhập)
        if (StringUtils.hasText(customer.getIdentityNumber())) {
            if (isNew && customerService.existsByIdentityNumber(customer.getIdentityNumber())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số CCCD/Hộ chiếu [" + customer.getIdentityNumber() + "] đã tồn tại trong hệ thống!");
                return "redirect:/admin/customers";
            } else if (!isNew && customerService.existsByIdentityNumberAndIdNot(customer.getIdentityNumber(), customer.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số CCCD/Hộ chiếu [" + customer.getIdentityNumber() + "] đã thuộc về khách hàng khác!");
                return "redirect:/admin/customers";
            }
        }

        // Nếu có lỗi validation
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu khách hàng không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:/admin/customers";
        }

        Customer savedCustomer = customerService.save(customer);

        String msg = isNew
                ? "Thêm mới hồ sơ khách hàng [" + savedCustomer.getCustomerCode() + " - " + savedCustomer.getFullName() + "] thành công!"
                : "Cập nhật hồ sơ khách hàng [" + savedCustomer.getCustomerCode() + "] thành công!";
        redirectAttributes.addFlashAttribute("successMessage", msg);

        return "redirect:/admin/customers";
    }

    @PostMapping("/create")
    public String createCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return saveCustomer(customer, bindingResult, redirectAttributes);
    }

    @PostMapping("/edit/{id}")
    public String editCustomer(
            @PathVariable Long id,
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        customer.setId(id);
        return saveCustomer(customer, bindingResult, redirectAttributes);
    }

    /**
     * Màn hình xem chi tiết hồ sơ khách hàng và thông tin liên kết.
     */
    @GetMapping({"/view/{id}", "/{id}"})
    public String viewCustomerDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return customerService.findById(id).map(customer -> {
            model.addAttribute("customer", customer);

            List<Booking> bookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
            model.addAttribute("bookings", bookings);

            long totalBookings = bookings.size();
            long totalNights = 0;
            BigDecimal totalSpent = BigDecimal.ZERO;

            for (Booking b : bookings) {
                if (b.getStatus() != Booking.BookingStatus.CANCELLED) {
                    if (b.getTotalAmount() != null) {
                        totalSpent = totalSpent.add(b.getTotalAmount());
                    }
                    if (b.getCheckIn() != null && b.getCheckOut() != null) {
                        long nights = Duration.between(b.getCheckIn(), b.getCheckOut()).toDays();
                        totalNights += (nights > 0 ? nights : 1);
                    }
                }
            }

            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("totalNights", totalNights);
            model.addAttribute("totalSpent", totalSpent);

            return "admin/customer/view";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hồ sơ khách hàng có ID: " + id);
            return "redirect:/admin/customers";
        });
    }

    /**
     * Khóa / Mở khóa hồ sơ khách hàng.
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return customerService.findById(id).map(customer -> {
            customerService.toggleStatus(id);
            String statusText = !customer.getActive() ? "Mở khóa" : "Khóa";
            redirectAttributes.addFlashAttribute("successMessage", statusText + " hồ sơ khách hàng [" + customer.getFullName() + "] thành công!");
            return "redirect:/admin/customers";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khách hàng!");
            return "redirect:/admin/customers";
        });
    }

    /**
     * Xóa hồ sơ khách hàng.
     */
    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa hồ sơ khách hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa hồ sơ khách hàng này do đã có dữ liệu ràng buộc liên quan (Đặt phòng/Hóa đơn). Hãy sử dụng tính năng Khóa hồ sơ!");
        }
        return "redirect:/admin/customers";
    }
}
