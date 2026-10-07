package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.HotelService;
import com.hotel.hotelmanagement.service.CloudinaryService;
import com.hotel.hotelmanagement.service.HotelServiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/services")
public class AdminServiceController {

    private static final String FOLDER_SERVICES = "hotel-management/services";

    private final HotelServiceService hotelServiceService;
    private final CloudinaryService cloudinaryService;

    public AdminServiceController(HotelServiceService hotelServiceService, CloudinaryService cloudinaryService) {
        this.hotelServiceService = hotelServiceService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public String listServices(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        
        List<HotelService> allServices = hotelServiceService.findAll();
        long totalCount = allServices.size();
        long activeCount = allServices.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count();
        long inactiveCount = totalCount - activeCount;

        int pageNum = Math.max(1, page);
        Page<HotelService> servicesPage = hotelServiceService.searchServices(name, status, PageRequest.of(pageNum - 1, 10, Sort.by(Sort.Direction.ASC, "id")));

        model.addAttribute("services", servicesPage.getContent());
        model.addAttribute("servicesPage", servicesPage);
        model.addAttribute("paramName", name);
        model.addAttribute("paramStatus", status);
        model.addAttribute("totalServices", totalCount);
        model.addAttribute("activeServices", activeCount);
        model.addAttribute("inactiveServices", inactiveCount);
        
        return "admin/service/list";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectServiceForm() {
        return "redirect:/admin/services";
    }

    @PostMapping("/create")
    public String createService(@Valid @ModelAttribute("service") HotelService service,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                RedirectAttributes redirectAttributes) {
        if (hotelServiceService.existsByName(service.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên dịch vụ '" + service.getName() + "' đã tồn tại!");
            return "redirect:/admin/services";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu dịch vụ không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:/admin/services";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadSingleImage(imageFile, FOLDER_SERVICES);
                service.setImageUrl(imageUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi upload ảnh: " + e.getMessage());
                return "redirect:/admin/services";
            }
        }

        hotelServiceService.save(service);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới dịch vụ '" + service.getName() + "' thành công!");
        return "redirect:/admin/services";
    }

    @PostMapping("/edit/{id}")
    public String editService(@PathVariable Long id,
                              @Valid @ModelAttribute("service") HotelService service,
                              BindingResult bindingResult,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes redirectAttributes) {
        if (hotelServiceService.existsByNameAndIdNot(service.getName(), id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên dịch vụ '" + service.getName() + "' đã thuộc về dịch vụ khác!");
            return "redirect:/admin/services";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu dịch vụ không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:/admin/services";
        }

        service.setId(id);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Xóa ảnh cũ trên Cloudinary nếu có ảnh mới
                hotelServiceService.findById(id).ifPresent(old -> {
                    if (old.getImageUrl() != null && !old.getImageUrl().isBlank()) {
                        cloudinaryService.deleteImageByUrl(old.getImageUrl());
                    }
                });

                String imageUrl = cloudinaryService.uploadSingleImage(imageFile, FOLDER_SERVICES);
                service.setImageUrl(imageUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi upload ảnh: " + e.getMessage());
                return "redirect:/admin/services";
            }
        } else {
            // Giữ lại URL ảnh cũ nếu không chọn file ảnh mới
            hotelServiceService.findById(id).ifPresent(old -> service.setImageUrl(old.getImageUrl()));
        }

        hotelServiceService.save(service);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật dịch vụ '" + service.getName() + "' thành công!");
        return "redirect:/admin/services";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        hotelServiceService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái thành công!");
        return "redirect:/admin/services";
    }
}
