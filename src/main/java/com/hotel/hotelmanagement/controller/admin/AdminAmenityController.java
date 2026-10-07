package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.Amenity;
import com.hotel.hotelmanagement.service.AmenityService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/amenities")
public class AdminAmenityController {

    private final AmenityService amenityService;

    public AdminAmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping
    public String listAmenities() {
        return "redirect:/admin/room-types?tab=amenities";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectAmenityForm() {
        return "redirect:/admin/room-types?tab=amenities";
    }

    @PostMapping("/create")
    public String createAmenity(@Valid @ModelAttribute("amenity") Amenity amenity,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (amenityService.existsByName(amenity.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên tiện nghi '" + amenity.getName() + "' đã tồn tại!");
            return "redirect:/admin/room-types?tab=amenities";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu tiện nghi không hợp lệ!");
            return "redirect:/admin/room-types?tab=amenities";
        }

        amenityService.save(amenity);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới tiện nghi '" + amenity.getName() + "' thành công!");
        return "redirect:/admin/room-types?tab=amenities";
    }

    @PostMapping("/edit/{id}")
    public String editAmenity(@PathVariable Long id,
                              @Valid @ModelAttribute("amenity") Amenity amenity,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (amenityService.existsByNameAndIdNot(amenity.getName(), id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên tiện nghi '" + amenity.getName() + "' đã thuộc về tiện nghi khác!");
            return "redirect:/admin/room-types?tab=amenities";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu tiện nghi không hợp lệ!");
            return "redirect:/admin/room-types?tab=amenities";
        }

        amenity.setId(id);
        amenityService.save(amenity);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tiện nghi '" + amenity.getName() + "' thành công!");
        return "redirect:/admin/room-types?tab=amenities";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        amenityService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái tiện nghi thành công!");
        return "redirect:/admin/room-types?tab=amenities";
    }
}
