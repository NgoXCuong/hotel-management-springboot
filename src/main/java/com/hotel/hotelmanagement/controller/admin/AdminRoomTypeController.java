package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.entity.Amenity;
import com.hotel.hotelmanagement.entity.RoomType;
import com.hotel.hotelmanagement.repository.AmenityRepository;
import com.hotel.hotelmanagement.service.AmenityService;
import com.hotel.hotelmanagement.service.CloudinaryService;
import com.hotel.hotelmanagement.service.RoomTypeService;
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

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/room-types")
public class AdminRoomTypeController {

    private static final String FOLDER_ROOM_TYPES = "hotel-management/room-types";

    private final RoomTypeService roomTypeService;
    private final AmenityService amenityService;
    private final AmenityRepository amenityRepository;
    private final CloudinaryService cloudinaryService;

    public AdminRoomTypeController(
            RoomTypeService roomTypeService,
            AmenityService amenityService,
            AmenityRepository amenityRepository,
            CloudinaryService cloudinaryService) {
        this.roomTypeService = roomTypeService;
        this.amenityService = amenityService;
        this.amenityRepository = amenityRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Màn hình Loại phòng & Tiện nghi: Tích hợp 2 tab Loại phòng và Tiện nghi gắn phòng
     */
    @GetMapping
    public String listRoomTypes(
            @RequestParam(defaultValue = "room-types") String tab,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1") int amenityPage,
            Model model) {
        int rtPageNum = Math.max(1, page);
        int amPageNum = Math.max(1, amenityPage);

        Page<RoomType> roomTypesPage = roomTypeService.findAll(PageRequest.of(rtPageNum - 1, 10, Sort.by(Sort.Direction.DESC, "id")));
        Page<Amenity> amenitiesPage = amenityService.findAll(PageRequest.of(amPageNum - 1, 10, Sort.by(Sort.Direction.ASC, "id")));

        model.addAttribute("roomTypes", roomTypesPage.getContent());
        model.addAttribute("roomTypesPage", roomTypesPage);
        model.addAttribute("amenities", amenitiesPage.getContent());
        model.addAttribute("amenitiesPage", amenitiesPage);
        model.addAttribute("allAmenities", amenityRepository.findAllByActiveTrueOrderByNameAsc());
        model.addAttribute("activeTab", tab);
        model.addAttribute("totalRoomTypes", roomTypesPage.getTotalElements());
        model.addAttribute("totalAmenities", amenitiesPage.getTotalElements());
        return "admin/room-type/list";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectRoomTypeForm() {
        return "redirect:/admin/room-types";
    }

    @PostMapping("/create")
    public String createRoomType(@Valid @ModelAttribute("roomType") RoomType roomType,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                 RedirectAttributes redirectAttributes) {
        if (roomTypeService.existsByName(roomType.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên loại phòng '" + roomType.getName() + "' đã tồn tại!");
            return "redirect:/admin/room-types?tab=room-types";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu loại phòng không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:/admin/room-types?tab=room-types";
        }

        List<String> uploadedUrls = new ArrayList<>();
        if (imageFiles != null && !imageFiles.isEmpty()) {
            try {
                uploadedUrls = cloudinaryService.uploadMultipleImages(imageFiles, FOLDER_ROOM_TYPES);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi upload ảnh: " + e.getMessage());
                return "redirect:/admin/room-types?tab=room-types";
            }
        }

        roomTypeService.save(roomType, uploadedUrls, null);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới loại phòng '" + roomType.getName() + "' thành công!");
        return "redirect:/admin/room-types?tab=room-types";
    }

    @PostMapping("/edit/{id}")
    public String editRoomType(@PathVariable Long id,
                               @Valid @ModelAttribute("roomType") RoomType roomType,
                               BindingResult bindingResult,
                               @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                               @RequestParam(value = "deletedImageUrls", required = false) List<String> deletedImageUrls,
                               RedirectAttributes redirectAttributes) {
        if (roomTypeService.existsByNameAndIdNot(roomType.getName(), id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên loại phòng '" + roomType.getName() + "' đã thuộc về loại phòng khác!");
            return "redirect:/admin/room-types?tab=room-types";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu loại phòng không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:/admin/room-types?tab=room-types";
        }

        List<String> newUploadedUrls = new ArrayList<>();
        if (imageFiles != null && !imageFiles.isEmpty()) {
            try {
                newUploadedUrls = cloudinaryService.uploadMultipleImages(imageFiles, FOLDER_ROOM_TYPES);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi upload ảnh: " + e.getMessage());
                return "redirect:/admin/room-types?tab=room-types";
            }
        }

        roomType.setId(id);
        roomTypeService.save(roomType, newUploadedUrls, deletedImageUrls);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại phòng '" + roomType.getName() + "' thành công!");
        return "redirect:/admin/room-types?tab=room-types";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roomTypeService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái loại phòng thành công!");
        return "redirect:/admin/room-types?tab=room-types";
    }
}
