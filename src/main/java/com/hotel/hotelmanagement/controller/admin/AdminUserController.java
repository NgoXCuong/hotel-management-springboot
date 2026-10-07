package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.Role;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.RoleRepository;
import com.hotel.hotelmanagement.service.CloudinaryService;
import com.hotel.hotelmanagement.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final String FOLDER_AVATARS = "hotel-management/avatars";

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final CloudinaryService cloudinaryService;

    public AdminUserController(UserService userService, RoleRepository roleRepository, CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Màn hình danh sách Tài khoản & Phân quyền (list.html)
     */
    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean accountNonLocked,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<User> usersPage = userService.searchUsers(
                keyword, roleId, enabled, accountNonLocked,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))
        );
        List<Role> roles = roleRepository.findAll();

        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("roles", roles);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("enabled", enabled);
        model.addAttribute("accountNonLocked", accountNonLocked);

        // KPI Counts
        model.addAttribute("totalUsers", userService.countTotal());
        model.addAttribute("activeUsers", userService.countActive());
        model.addAttribute("lockedUsers", userService.countLocked());

        return "admin/user/list";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectUserForm() {
        return "redirect:/admin/users";
    }

    /**
     * Xử lý Cấp mới tài khoản từ Modal
     */
    @PostMapping("/create")
    public String createUser(
            @ModelAttribute User user,
            @RequestParam(name = "roleIds", required = false) List<Long> roleIds,
            @RequestParam(name = "rawPassword") String rawPassword,
            @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes) {
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarUrl = cloudinaryService.uploadSingleImage(avatarFile, FOLDER_AVATARS);
                user.setAvatarUrl(avatarUrl);
            }

            userService.createUser(user, roleIds, rawPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Cấp tài khoản mới cho nhân viên '" + user.getUsername() + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Xử lý Cập nhật tài khoản từ Modal
     */
    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute User user,
            @RequestParam(name = "roleIds", required = false) List<Long> roleIds,
            @RequestParam(name = "newRawPassword", required = false) String newRawPassword,
            @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes) {
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                // Xóa avatar cũ trên Cloudinary nếu có
                userService.findById(id).ifPresent(oldUser -> {
                    if (oldUser.getAvatarUrl() != null && !oldUser.getAvatarUrl().isBlank()) {
                        cloudinaryService.deleteImageByUrl(oldUser.getAvatarUrl());
                    }
                });
                String avatarUrl = cloudinaryService.uploadSingleImage(avatarFile, FOLDER_AVATARS);
                user.setAvatarUrl(avatarUrl);
            } else {
                // Giữ lại URL avatar cũ nếu người dùng không chọn file mới
                userService.findById(id).ifPresent(oldUser -> user.setAvatarUrl(oldUser.getAvatarUrl()));
            }

            userService.updateUser(id, user, roleIds, newRawPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản '" + user.getUsername() + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Bật/Tắt khóa tài khoản (Khóa hoặc Mở khóa do đăng nhập sai)
     */
    @PostMapping("/toggle-lock/{id}")
    public String toggleLock(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/admin/users") String returnUrl,
            RedirectAttributes redirectAttributes) {
        try {
            userService.toggleAccountLock(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái khóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    /**
     * Bật/Tắt kích hoạt tài khoản
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/admin/users") String returnUrl,
            RedirectAttributes redirectAttributes) {
        try {
            userService.toggleEnabled(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hoạt động thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    /**
     * Cấp lại mật khẩu cho tài khoản
     */
    @PostMapping("/reset-password/{id}")
    public String resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            @RequestParam(defaultValue = "/admin/users") String returnUrl,
            RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Cấp lại mật khẩu mới cho tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cấp lại mật khẩu: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }
}
