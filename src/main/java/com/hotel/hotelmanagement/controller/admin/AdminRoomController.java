package com.hotel.hotelmanagement.controller.admin;

import com.hotel.hotelmanagement.dto.RoomMatrixStatsDTO;
import com.hotel.hotelmanagement.dto.RoomTileDTO;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Room;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.RoomMatrixService;
import com.hotel.hotelmanagement.service.RoomService;
import com.hotel.hotelmanagement.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final RoomMatrixService roomMatrixService;
    private final UserRepository userRepository;

    public AdminRoomController(
            RoomService roomService,
            RoomTypeService roomTypeService,
            RoomMatrixService roomMatrixService,
            UserRepository userRepository) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
        this.roomMatrixService = roomMatrixService;
        this.userRepository = userRepository;
    }

    /**
     * Chuyển hướng màn hình Sơ đồ phòng về trang Quản lý phòng chung
     */
    @GetMapping({"/matrix", "/map"})
    public String showRoomMatrix(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String status,
            Model model) {
        return listRooms(roomNumber, floor, roomTypeId, status, "grid", 1, model);
    }

    /**
     * Cập nhật nhanh trạng thái phòng từ Sơ đồ phòng
     */
    @PostMapping("/quick-update-status")
    public String quickUpdateStatus(
            @RequestParam Long roomId,
            @RequestParam Room.RoomStatus newStatus,
            @RequestParam(defaultValue = "/admin/rooms?view=grid") String returnUrl,
            RedirectAttributes redirectAttributes) {
        try {
            roomMatrixService.updateRoomStatus(roomId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái phòng sang: " + newStatus.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    /**
     * Đặt phòng nhanh trực tiếp từ ô phòng trống trên sơ đồ
     */
    @PostMapping("/quick-book")
    public String quickBook(
            @RequestParam Long roomId,
            @RequestParam String guestName,
            @RequestParam String guestPhone,
            @RequestParam(required = false) String guestEmail,
            @RequestParam(required = false) String guestIdentity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
            @RequestParam(defaultValue = "1") Integer guests,
            @RequestParam(defaultValue = "0") BigDecimal deposit,
            @RequestParam(defaultValue = "grid") String view,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = null;
            if (authentication != null) {
                currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            }

            Booking booking = roomMatrixService.quickBookRoom(
                    roomId, guestName, guestPhone, guestEmail, guestIdentity,
                    checkIn, checkOut, guests, deposit, currentUser
            );

            redirectAttributes.addFlashAttribute("successMessage", "Đã tiếp nhận nhận phòng nhanh cho đơn " + booking.getBookingCode() + " thành công!");
            return "redirect:/admin/bookings/" + booking.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt phòng nhanh: " + e.getMessage());
            return "redirect:/admin/rooms?view=" + view;
        }
    }

    /**
     * Màn hình Quản lý Phòng thống nhất (Sơ đồ trực quan Floor Plan & Bảng danh sách CRUD)
     */
    @GetMapping
    public String listRooms(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "grid") String view,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        Room.RoomStatus roomStatus = null;
        if (org.springframework.util.StringUtils.hasText(status)) {
            try {
                roomStatus = Room.RoomStatus.valueOf(status.trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        // 1. Dữ liệu cho Sơ đồ trực quan (Floor Plan)
        Map<Integer, List<RoomTileDTO>> matrixByFloor = roomMatrixService.getRoomMatrixByFloor(roomNumber, floor, roomTypeId, roomStatus);
        RoomMatrixStatsDTO stats = roomMatrixService.getRoomMatrixStats();

        // 2. Dữ liệu cho Bảng danh sách quản trị (List CRUD) có phân trang
        int pageNum = Math.max(1, page);
        org.springframework.data.domain.Page<Room> roomsPage = roomService.searchRooms(
                roomNumber, floor, roomTypeId, status,
                org.springframework.data.domain.PageRequest.of(pageNum - 1, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "roomNumber"))
        );

        model.addAttribute("matrixByFloor", matrixByFloor);
        model.addAttribute("stats", stats);
        model.addAttribute("rooms", roomsPage.getContent());
        model.addAttribute("roomsPage", roomsPage);
        model.addAttribute("roomTypes", roomTypeService.findAll());
        model.addAttribute("floors", roomService.findDistinctFloors());
        model.addAttribute("statuses", Room.RoomStatus.values());
        model.addAttribute("currentView", view);

        // Thống kê nhanh
        long total = stats.getTotalRooms();
        long available = stats.getAvailableRooms();
        long occupied = stats.getOccupiedRooms();
        long cleaning = stats.getCleaningRooms();
        long maintenance = stats.getMaintenanceRooms();

        model.addAttribute("totalRooms", total);
        model.addAttribute("availableRoomsCount", available);
        model.addAttribute("occupiedRoomsCount", occupied);
        model.addAttribute("cleaningRoomsCount", cleaning);
        model.addAttribute("maintenanceRoomsCount", maintenance);

        model.addAttribute("paramRoomNumber", roomNumber);
        model.addAttribute("paramFloor", floor);
        model.addAttribute("paramRoomTypeId", roomTypeId);
        model.addAttribute("paramStatus", status);

        return "admin/room/list";
    }

    @GetMapping({"/create", "/edit/{id}"})
    public String redirectRoomForm() {
        return "redirect:/admin/rooms";
    }

    @PostMapping("/create")
    public String createRoom(@Valid @ModelAttribute("room") Room room,
                             BindingResult bindingResult,
                             @RequestParam(defaultValue = "/admin/rooms") String returnUrl,
                             RedirectAttributes redirectAttributes) {
        if (roomService.existsByRoomNumber(room.getRoomNumber())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số phòng '" + room.getRoomNumber() + "' đã tồn tại!");
            return "redirect:" + returnUrl;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu phòng không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:" + returnUrl;
        }

        roomService.save(room);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới phòng " + room.getRoomNumber() + " thành công!");
        return "redirect:" + returnUrl;
    }

    @PostMapping("/edit/{id}")
    public String editRoom(@PathVariable Long id,
                           @Valid @ModelAttribute("room") Room room,
                           BindingResult bindingResult,
                           @RequestParam(defaultValue = "/admin/rooms") String returnUrl,
                           RedirectAttributes redirectAttributes) {
        if (roomService.existsByRoomNumberAndIdNot(room.getRoomNumber(), id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số phòng '" + room.getRoomNumber() + "' đã thuộc về phòng khác!");
            return "redirect:" + returnUrl;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu phòng không hợp lệ. Vui lòng kiểm tra lại!");
            return "redirect:" + returnUrl;
        }

        room.setId(id);
        roomService.save(room);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng " + room.getRoomNumber() + " thành công!");
        return "redirect:" + returnUrl;
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, 
                               @RequestParam(defaultValue = "/admin/rooms") String returnUrl,
                               RedirectAttributes redirectAttributes) {
        roomService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái phòng thành công!");
        return "redirect:" + returnUrl;
    }

    @PostMapping("/quick-status/{id}")
    public String quickStatus(@PathVariable Long id,
                              @RequestParam Room.RoomStatus status,
                              @RequestParam(defaultValue = "grid") String view,
                              RedirectAttributes redirectAttributes) {
        roomMatrixService.updateRoomStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái phòng thành công!");
        return "redirect:/admin/rooms?view=" + view;
    }
}
