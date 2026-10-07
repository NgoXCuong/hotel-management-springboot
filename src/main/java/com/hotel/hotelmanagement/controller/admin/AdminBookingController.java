package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.*;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;
    private final CustomerService customerService;
    private final RoomTypeService roomTypeService;
    private final PromotionService promotionService;
    private final HotelServiceService hotelServiceService;
    private final UserRepository userRepository;

    public AdminBookingController(
            BookingService bookingService,
            CustomerService customerService,
            RoomTypeService roomTypeService,
            PromotionService promotionService,
            HotelServiceService hotelServiceService,
            UserRepository userRepository) {
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.roomTypeService = roomTypeService;
        this.promotionService = promotionService;
        this.hotelServiceService = hotelServiceService;
        this.userRepository = userRepository;
    }

    /**
     * Màn hình danh sách đơn đặt phòng + Bộ lọc đa chiều
     */
    @GetMapping
    public String listBookings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Booking.BookingStatus status,
            @RequestParam(required = false) Booking.BookingSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<Booking> bookingsPage = bookingService.searchBookings(
                keyword, status, source, checkInFrom, checkInTo,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("bookings", bookingsPage.getContent());
        model.addAttribute("bookingsPage", bookingsPage);
        model.addAttribute("statuses", Booking.BookingStatus.values());
        model.addAttribute("sources", Booking.BookingSource.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("source", source);
        model.addAttribute("checkInFrom", checkInFrom);
        model.addAttribute("checkInTo", checkInTo);

        // Thống kê nhanh KPI
        model.addAttribute("totalBookings", bookingService.countTotal());
        model.addAttribute("checkedInCount", bookingService.countCheckedIn());
        model.addAttribute("pendingConfirmedCount", bookingService.countPendingOrConfirmed());
        model.addAttribute("totalRevenue", bookingService.calculateTotalRevenue());

        return "admin/booking/list";
    }

    /**
     * Màn hình Tạo mới Đặt phòng tại quầy (create.html)
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        LocalDateTime defaultCheckIn = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0));
        LocalDateTime defaultCheckOut = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(12, 0));

        Booking booking = Booking.builder()
                .checkIn(defaultCheckIn)
                .checkOut(defaultCheckOut)
                .numberOfGuests(1)
                .source(Booking.BookingSource.WALK_IN)
                .status(Booking.BookingStatus.CONFIRMED)
                .depositAmount(BigDecimal.ZERO)
                .build();

        model.addAttribute("booking", booking);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("roomTypes", roomTypeService.findAll());
        model.addAttribute("sources", Booking.BookingSource.values());
        model.addAttribute("availableRooms", bookingService.findAvailableRooms(defaultCheckIn, defaultCheckOut, null, null));

        return "admin/booking/create";
    }

    /**
     * Xử lý lưu đơn đặt phòng mới tại quầy
     */
    @PostMapping("/create")
    public String saveBooking(
            @ModelAttribute("booking") Booking booking,
            @RequestParam(required = false) List<Long> roomIds,
            @RequestParam(required = false) String voucherCode,
            @RequestParam(defaultValue = "false") Boolean isNewCustomer,
            @RequestParam(required = false) String newCustomerName,
            @RequestParam(required = false) String newCustomerPhone,
            @RequestParam(required = false) String newCustomerEmail,
            @RequestParam(required = false) String newCustomerIdentityNumber,
            RedirectAttributes redirectAttributes) {

        try {
            // 1. Kiểm tra thời gian checkIn < checkOut
            if (booking.getCheckIn() == null || booking.getCheckOut() == null || !booking.getCheckOut().isAfter(booking.getCheckIn())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thời gian trả phòng (Check-out) phải sau thời gian nhận phòng (Check-in)!");
                return "redirect:/admin/bookings/create";
            }

            // 2. Xử lý khách hàng mới (nếu chọn nhập nhanh tại chỗ)
            if (Boolean.TRUE.equals(isNewCustomer)) {
                if (!StringUtils.hasText(newCustomerName) || !StringUtils.hasText(newCustomerPhone)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ Họ tên và Số điện thoại của khách hàng mới!");
                    return "redirect:/admin/bookings/create";
                }

                // Kiểm tra trùng SĐT
                if (customerService.existsByPhone(newCustomerPhone.trim())) {
                    // Lấy khách hàng cũ nếu đã có số này
                    Optional<Customer> existing = customerService.findByPhone(newCustomerPhone.trim());
                    if (existing.isPresent()) {
                        booking.setCustomer(existing.get());
                    }
                } else {
                    Customer newCustomer = Customer.builder()
                            .fullName(newCustomerName.trim())
                            .phone(newCustomerPhone.trim())
                            .email(StringUtils.hasText(newCustomerEmail) ? newCustomerEmail.trim() : null)
                            .identityNumber(StringUtils.hasText(newCustomerIdentityNumber) ? newCustomerIdentityNumber.trim() : null)
                            .active(true)
                            .build();
                    Customer savedCustomer = customerService.save(newCustomer);
                    booking.setCustomer(savedCustomer);
                }
            }

            // 3. Kiểm tra khách hàng
            if (booking.getCustomer() == null || booking.getCustomer().getId() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn hoặc thêm thông tin khách hàng đặt phòng!");
                return "redirect:/admin/bookings/create";
            }

            // 4. Kiểm tra phòng chọn
            if (roomIds == null || roomIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một phòng khả dụng cho đơn đặt!");
                return "redirect:/admin/bookings/create";
            }

            // 5. Tạo đơn đặt phòng
            Booking created = bookingService.createBooking(booking, roomIds, voucherCode);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo đơn đặt phòng " + created.getBookingCode() + " thành công!");
            return "redirect:/admin/bookings/" + created.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo đơn đặt phòng: " + e.getMessage());
            return "redirect:/admin/bookings/create";
        }
    }

    /**
     * Màn hình Chi tiết Đặt phòng & Vận hành Lễ tân (detail.html)
     */
    @GetMapping({"/{id}", "/view/{id}"})
    public String showBookingDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Booking> bookingOpt = bookingService.findById(id);
        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt phòng có ID: " + id);
            return "redirect:/admin/bookings";
        }

        Booking booking = bookingOpt.get();
        model.addAttribute("booking", booking);

        // Danh sách các dịch vụ khách sạn đang hoạt động phục vụ Modal Thêm dịch vụ
        List<HotelService> activeServices = hotelServiceService.searchServices(null, "active");
        model.addAttribute("activeServices", activeServices);

        return "admin/booking/detail";
    }

    /**
     * Tiếp nhận nhận phòng (Check-in)
     */
    @PostMapping({"/{id}/check-in", "/check-in/{id}"})
    public String checkInBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking b = bookingService.checkIn(id);
            redirectAttributes.addFlashAttribute("successMessage", "Khách đã nhận phòng (Check-in) thành công cho đơn " + b.getBookingCode() + "! Phòng đã chuyển sang trạng thái ĐANG CÓ KHÁCH.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi Check-in: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    /**
     * Thêm dịch vụ phát sinh vào đơn đặt phòng
     */
    @PostMapping("/{id}/add-service")
    public String addServiceToBooking(
            @PathVariable("id") Long id,
            @RequestParam Long serviceId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String note,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = null;
            if (authentication != null) {
                currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            }

            Booking b = bookingService.addServiceToBooking(id, serviceId, quantity, note, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm dịch vụ phát sinh vào đơn " + b.getBookingCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    /**
     * Xóa dịch vụ phát sinh khỏi đơn đặt phòng
     */
    @PostMapping("/{id}/remove-service/{serviceItemId}")
    public String removeServiceFromBooking(
            @PathVariable("id") Long id,
            @PathVariable("serviceItemId") Long serviceItemId,
            RedirectAttributes redirectAttributes) {
        try {
            Booking b = bookingService.removeServiceFromBooking(id, serviceItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa dịch vụ và cập nhật lại tổng tiền đơn " + b.getBookingCode() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    /**
     * Tiến hành trả phòng (Check-out)
     */
    @PostMapping({"/{id}/check-out", "/check-out/{id}"})
    public String checkOutBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking b = bookingService.checkOut(id);
            redirectAttributes.addFlashAttribute("successMessage", "Khách đã hoàn tất trả phòng (Check-out) cho đơn " + b.getBookingCode() + "! Các phòng đã chuyển sang trạng thái DỌN DẸP.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi Check-out: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    /**
     * Duyệt đơn đặt phòng (Confirm)
     */
    @PostMapping({"/{id}/confirm", "/confirm/{id}"})
    public String confirmBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking b = bookingService.confirmBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt đơn đặt phòng " + b.getBookingCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    /**
     * Hủy đơn đặt phòng
     */
    @PostMapping({"/{id}/cancel", "/cancel/{id}"})
    public String cancelBooking(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "Khách hàng yêu cầu hủy đơn") String reason,
            RedirectAttributes redirectAttributes) {
        try {
            Booking b = bookingService.cancelBooking(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn đặt phòng " + b.getBookingCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    // ================= REST APIs CHO GIAO DIỆN TƯƠNG TÁC =================

    /**
     * API AJAX tải danh sách phòng trống theo ngày và loại phòng
     */
    @GetMapping("/api/available-rooms")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
            @RequestParam(required = false) Long roomTypeId) {

        List<Room> rooms = bookingService.findAvailableRooms(checkIn, checkOut, roomTypeId, null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Room r : rooms) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("roomNumber", r.getRoomNumber());
            map.put("floor", r.getFloor());
            map.put("roomTypeName", r.getRoomType() != null ? r.getRoomType().getName() : "Tiêu chuẩn");
            map.put("pricePerNight", r.getRoomType() != null ? r.getRoomType().getPricePerNight() : BigDecimal.ZERO);
            map.put("maxGuests", r.getRoomType() != null ? r.getRoomType().getMaxGuests() : 2);
            map.put("status", r.getStatus().name());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * API AJAX kiểm tra mã Voucher và trả về mức giảm giá
     */
    @GetMapping("/api/check-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {

        Map<String, Object> response = new HashMap<>();
        if (!StringUtils.hasText(code)) {
            response.put("valid", false);
            response.put("message", "Vui lòng nhập mã khuyến mãi");
            return ResponseEntity.ok(response);
        }

        Optional<Promotion> promoOpt = promotionService.findByCode(code.trim());
        if (promoOpt.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Mã khuyến mãi không tồn tại trong hệ thống");
            return ResponseEntity.ok(response);
        }

        Promotion promo = promoOpt.get();
        if (!promotionService.checkValidity(code.trim(), amount)) {
            response.put("valid", false);
            response.put("message", "Mã khuyến mãi không thỏa điều kiện áp dụng (chưa tới ngày, hết hạn, hết lượt dùng hoặc chưa đạt giá trị đơn tối thiểu)");
            return ResponseEntity.ok(response);
        }

        BigDecimal discount = promotionService.calculateDiscount(promo, amount);
        BigDecimal finalAmount = amount.subtract(discount).max(BigDecimal.ZERO);

        response.put("valid", true);
        response.put("code", promo.getCode());
        response.put("name", promo.getName());
        response.put("discountAmount", discount);
        response.put("finalAmount", finalAmount);
        response.put("message", "Áp dụng mã khuyến mãi thành công! Tiết kiệm: " + String.format("%,.0f đ", discount));

        return ResponseEntity.ok(response);
    }
}
