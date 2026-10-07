package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.entity.Role;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.repository.RoleRepository;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.CustomerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * Controller điều hướng và xử lý xác thực tài khoản:
 * - /auth/login: Trang đăng nhập
 * - /auth/register: Trang đăng ký tài khoản khách hàng
 * - /auth/forgot-password: Trang quên mật khẩu
 * - /auth/403: Trang báo lỗi phân quyền
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            RoleRepository roleRepository,
            CustomerService customerService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Màn hình Đăng nhập (login.html)
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "Quý khách đã đăng xuất thành công.");
        }

        return "auth/login";
    }

    /**
     * Màn hình Đăng ký (register.html)
     */
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    /**
     * Xử lý Đăng ký tài khoản khách hàng mới
     */
    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam(required = false) String identityNumber,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
                return "redirect:/auth/register";
            }

            if (password.length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu phải có tối thiểu 6 ký tự!");
                return "redirect:/auth/register";
            }

            if (userRepository.existsByUsername(username.trim())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập '" + username + "' đã được sử dụng!");
                return "redirect:/auth/register";
            }

            if (userRepository.existsByEmail(email.trim())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + email + "' đã được đăng ký tài khoản!");
                return "redirect:/auth/register";
            }

            // Gán ROLE_CUSTOMER
            Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng").build()));

            User newUser = User.builder()
                    .username(username.trim())
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName.trim())
                    .email(email.trim())
                    .phone(phone.trim())
                    .enabled(true)
                    .accountNonLocked(true)
                    .failedLoginAttempts(0)
                    .roles(Set.of(customerRole))
                    .build();

            User savedUser = userRepository.save(newUser);

            // Tạo hồ sơ Customer tương ứng
            Customer newCustomer = Customer.builder()
                    .user(savedUser)
                    .customerCode(customerService.generateCustomerCode())
                    .fullName(fullName.trim())
                    .phone(phone.trim())
                    .email(email.trim())
                    .identityNumber(StringUtils.hasText(identityNumber) ? identityNumber.trim() : null)
                    .active(true)
                    .build();

            customerRepository.save(newCustomer);

            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Quý khách có thể đăng nhập ngay.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đăng ký: " + e.getMessage());
            return "redirect:/auth/register";
        }
    }

    /**
     * Màn hình Quên mật khẩu (forgot-password.html)
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    /**
     * Xử lý gửi yêu cầu Quên mật khẩu
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        if (!StringUtils.hasText(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập địa chỉ email!");
            return "redirect:/auth/forgot-password";
        }

        boolean exists = userRepository.existsByEmail(email.trim());
        if (exists) {
            redirectAttributes.addFlashAttribute("successMessage", "Hướng dẫn khôi phục mật khẩu đã được gửi đến email của Quý khách.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản nào gắn với địa chỉ email này!");
        }

        return "redirect:/auth/forgot-password";
    }

    /**
     * Màn hình lỗi 403 (403.html)
     */
    @RequestMapping("/403")
    public String accessDenied() {
        return "auth/403";
    }
}
