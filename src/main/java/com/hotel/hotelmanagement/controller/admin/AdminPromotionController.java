package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.Promotion;
import com.hotel.hotelmanagement.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/promotions")
public class AdminPromotionController {

    private final PromotionService promotionService;

    public AdminPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * Danh sách mã khuyến mãi / voucher kèm bộ lọc và thống kê nhanh.
     */
    @GetMapping
    public String listPromotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Promotion.DiscountType discountType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<Promotion> promotionsPage = promotionService.searchPromotions(
                keyword, discountType, status,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("promotions", promotionsPage.getContent());
        model.addAttribute("promotionsPage", promotionsPage);
        model.addAttribute("discountTypes", Promotion.DiscountType.values());

        // Thống kê nhanh KPI
        model.addAttribute("totalCount", promotionService.countTotal());
        model.addAttribute("runningCount", promotionService.countRunning());
        model.addAttribute("expiredCount", promotionService.countExpired());

        // Trạng thái bộ lọc
        model.addAttribute("paramKeyword", keyword);
        model.addAttribute("paramDiscountType", discountType);
        model.addAttribute("paramStatus", status);

        return "admin/promotion/list";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectPromotionForm() {
        return "redirect:/admin/promotions";
    }

    /**
     * Xử lý lưu chương trình khuyến mãi từ Modal Dialog (áp dụng cho cả Tạo mới và Cập nhật).
     */
    @PostMapping("/save")
    public String savePromotion(
            @Valid @ModelAttribute("promotion") Promotion promotion,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        boolean isNew = (promotion.getId() == null);

        // Chuẩn hóa mã code thành chữ in hoa không khoảng trắng
        if (StringUtils.hasText(promotion.getCode())) {
            promotion.setCode(promotion.getCode().trim().toUpperCase().replaceAll("\\s+", ""));
        }

        // 1. Kiểm tra trùng lặp mã code
        if (StringUtils.hasText(promotion.getCode())) {
            if (isNew && promotionService.existsByCode(promotion.getCode())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher [" + promotion.getCode() + "] đã tồn tại trong hệ thống!");
                return "redirect:/admin/promotions";
            } else if (!isNew && promotionService.existsByCodeAndIdNot(promotion.getCode(), promotion.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher [" + promotion.getCode() + "] đã thuộc về chương trình khác!");
                return "redirect:/admin/promotions";
            }
        }

        // 2. Kiểm tra thời gian kết thúc phải sau thời gian bắt đầu
        if (promotion.getStartDate() != null && promotion.getEndDate() != null) {
            if (!promotion.getEndDate().isAfter(promotion.getStartDate())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
                return "redirect:/admin/promotions";
            }
        }

        // 3. Kiểm tra ràng buộc giảm giá theo phần trăm
        if (promotion.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            if (promotion.getDiscountValue() != null && promotion.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mức giảm theo phần trăm không được vượt quá 100%!");
                return "redirect:/admin/promotions";
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu khuyến mãi không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:/admin/promotions";
        }

        Promotion saved = promotionService.save(promotion);

        String msg = isNew
                ? "Tạo mới mã khuyến mãi [" + saved.getCode() + " - " + saved.getName() + "] thành công!"
                : "Cập nhật mã khuyến mãi [" + saved.getCode() + "] thành công!";
        redirectAttributes.addFlashAttribute("successMessage", msg);

        return "redirect:/admin/promotions";
    }

    @PostMapping("/create")
    public String createPromotion(
            @Valid @ModelAttribute("promotion") Promotion promotion,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return savePromotion(promotion, bindingResult, redirectAttributes);
    }

    @PostMapping("/edit/{id}")
    public String editPromotion(
            @PathVariable Long id,
            @Valid @ModelAttribute("promotion") Promotion promotion,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        promotion.setId(id);
        return savePromotion(promotion, bindingResult, redirectAttributes);
    }

    /**
     * Khóa / Mở khóa voucher.
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return promotionService.findById(id).map(promotion -> {
            promotionService.toggleStatus(id);
            String statusText = !promotion.getActive() ? "Kích hoạt" : "Tạm dừng";
            redirectAttributes.addFlashAttribute("successMessage", statusText + " mã voucher [" + promotion.getCode() + "] thành công!");
            return "redirect:/admin/promotions";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy chương trình khuyến mãi!");
            return "redirect:/admin/promotions";
        });
    }

    /**
     * Xóa chương trình khuyến mãi.
     */
    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa chương trình khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa voucher này do đã có đơn đặt phòng áp dụng. Hãy chuyển sang Tạm dừng!");
        }
        return "redirect:/admin/promotions";
    }
}
