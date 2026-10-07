package com.hotel.hotelmanagement.controller.client;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.entity.Notification;
import com.hotel.hotelmanagement.entity.Review;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.repository.ReviewRepository;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.NotificationService;
import com.hotel.hotelmanagement.service.ReviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller quản lý trung tâm tài khoản khách hàng (my-account.html).
 * Xử lý: Hồ sơ, Lịch sử đặt phòng, Hủy phòng, Đánh giá, Đổi mật khẩu, Thông báo.
 */
@Controller
public class PublicAccountController {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public PublicAccountController(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            BookingRepository bookingRepository,
            BookingService bookingService,
            ReviewRepository reviewRepository,
            ReviewService reviewService,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    /**
     * Màn hình chính Trung tâm Tài khoản (my-account.html)
     */
    @GetMapping("/my-account")
    public String myAccount(
            @RequestParam(defaultValue = "profile") String tab,
            Authentication authentication,
            Model model) {

        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElse(customerRepository.findFirstByEmail(user.getEmail()).orElse(null));

        List<Booking> bookings = (customer != null)
                ? bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                : List.of();

        List<Review> customerReviews = (customer != null)
                ? reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                : List.of();

        List<Notification> notifications = notificationService.getNotificationsForUser(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookings);
        model.addAttribute("reviews", customerReviews);
        model.addAttribute("notifications", notifications);
        model.addAttribute("activeTab", tab);

        return "client/my-account";
    }

    /**
     * Chuyển hướng /my-bookings sang /my-account?tab=bookings
     */
    @GetMapping("/my-bookings")
    public String redirectMyBookings() {
        return "redirect:/my-account?tab=bookings";
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân
     */
    @PostMapping("/my-account/profile")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String identityNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(required = false) Customer.Gender gender,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/auth/login";
        }

        try {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            Customer customer = customerRepository.findByUserId(user.getId())
                    .orElse(customerRepository.findFirstByEmail(user.getEmail()).orElse(null));

            if (customer == null) {
                customer = Customer.builder()
                        .user(user)
                        .customerCode("CUST-" + System.currentTimeMillis() % 1000000)
                        .fullName(fullName.trim())
                        .phone(phone.trim())
                        .email(email != null ? email.trim() : user.getEmail())
                        .identityNumber(identityNumber != null ? identityNumber.trim() : null)
                        .address(address)
                        .dateOfBirth(dateOfBirth)
                        .gender(gender)
                        .active(true)
                        .build();
            } else {
                customer.setFullName(fullName.trim());
                customer.setPhone(phone.trim());
                if (StringUtils.hasText(email)) customer.setEmail(email.trim());
                if (StringUtils.hasText(identityNumber)) customer.setIdentityNumber(identityNumber.trim());
                customer.setAddress(address);
                customer.setDateOfBirth(dateOfBirth);
                customer.setGender(gender);
            }

            customerRepository.save(customer);

            // Cập nhật User tương ứng
            user.setFullName(fullName.trim());
            user.setPhone(phone.trim());
            if (StringUtils.hasText(email)) user.setEmail(email.trim());
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật hồ sơ: " + e.getMessage());
        }

        return "redirect:/my-account?tab=profile";
    }

    /**
     * Đổi mật khẩu tài khoản
     */
    @PostMapping("/my-account/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/auth/login";
        }

        try {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không chính xác!");
                return "redirect:/my-account?tab=password";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận mới không khớp!");
                return "redirect:/my-account?tab=password";
            }

            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có tối thiểu 6 ký tự!");
                return "redirect:/my-account?tab=password";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đổi mật khẩu: " + e.getMessage());
        }

        return "redirect:/my-account?tab=password";
    }

    /**
     * Hủy đơn đặt phòng (Khách hàng tự hủy các đơn PENDING hoặc CONFIRMED)
     */
    @PostMapping("/my-account/bookings/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Khách hàng tự hủy trực tuyến") String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/auth/login";
        }

        try {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng!"));

            // Kiểm tra quyền: Đơn phải thuộc sở hữu của khách
            if (booking.getCustomer() != null && booking.getCustomer().getUser() != null) {
                if (!booking.getCustomer().getUser().getId().equals(user.getId())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thao tác trên đơn đặt phòng này!");
                    return "redirect:/my-account?tab=bookings";
                }
            }

            if (booking.getStatus() != Booking.BookingStatus.PENDING && booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể hủy đơn đặt phòng ở trạng thái Chờ duyệt hoặc Đã duyệt!");
                return "redirect:/my-account?tab=bookings";
            }

            bookingService.cancelBooking(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn đặt phòng #" + booking.getBookingCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy đơn: " + e.getMessage());
        }

        return "redirect:/my-account?tab=bookings";
    }

    /**
     * Gửi đánh giá cho chuyến đi đã hoàn tất
     */
    @PostMapping("/my-account/reviews")
    public String submitReview(
            @RequestParam Long bookingId,
            @RequestParam Integer rating,
            @RequestParam String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/auth/login";
        }

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng!"));

            if (booking.getStatus() != Booking.BookingStatus.CHECKED_OUT) {
                redirectAttributes.addFlashAttribute("errorMessage", "Quý khách chỉ có thể đánh giá sau khi đã hoàn tất trả phòng!");
                return "redirect:/my-account?tab=bookings";
            }

            Optional<Review> existingReview = reviewRepository.findByBookingId(bookingId);
            if (existingReview.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đơn đặt phòng này đã được gửi đánh giá trước đó!");
                return "redirect:/my-account?tab=bookings";
            }

            reviewService.createReview(bookingId, booking.getCustomer().getId(), rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn Quý khách đã chia sẻ trải nghiệm tuyệt vời!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/my-account?tab=bookings";
    }
}
