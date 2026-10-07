package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.dto.ReviewStatsDTO;
import com.hotel.hotelmanagement.entity.Review;
import com.hotel.hotelmanagement.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Màn hình danh sách Đánh giá & Phản hồi (list.html)
     */
    @GetMapping
    public String listReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<Review> reviewsPage = reviewService.searchReviews(
                keyword, rating, isVisible,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        );
        ReviewStatsDTO stats = reviewService.getReviewStats();

        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("reviewsPage", reviewsPage);
        model.addAttribute("stats", stats);
        model.addAttribute("keyword", keyword);
        model.addAttribute("rating", rating);
        model.addAttribute("isVisible", isVisible);

        return "admin/review/list";
    }

    /**
     * Bật/Tắt hiển thị công khai đánh giá
     */
    @PostMapping("/toggle-visibility/{id}")
    public String toggleVisibility(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/admin/reviews") String returnUrl,
            RedirectAttributes redirectAttributes) {
        try {
            reviewService.toggleVisibility(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị đánh giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    /**
     * Xóa đánh giá vi phạm tiêu chuẩn cộng đồng
     */
    @PostMapping("/delete/{id}")
    public String deleteReview(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/admin/reviews") String returnUrl,
            RedirectAttributes redirectAttributes) {
        try {
            reviewService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đánh giá vi phạm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa đánh giá: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }
}
