package com.hotel.hotelmanagement.controller.client;

import com.hotel.hotelmanagement.dto.ReviewStatsDTO;
import com.hotel.hotelmanagement.entity.Amenity;
import com.hotel.hotelmanagement.entity.HotelService;
import com.hotel.hotelmanagement.entity.Review;
import com.hotel.hotelmanagement.entity.RoomType;
import com.hotel.hotelmanagement.repository.AmenityRepository;
import com.hotel.hotelmanagement.repository.HotelServiceRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.ReviewService;
import com.hotel.hotelmanagement.service.RoomTypeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class PublicRoomController {

    private final RoomTypeService roomTypeService;
    private final AmenityRepository amenityRepository;
    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final HotelServiceRepository hotelServiceRepository;

    public PublicRoomController(
            RoomTypeService roomTypeService,
            AmenityRepository amenityRepository,
            ReviewService reviewService,
            BookingService bookingService,
            HotelServiceRepository hotelServiceRepository) {
        this.roomTypeService = roomTypeService;
        this.amenityRepository = amenityRepository;
        this.reviewService = reviewService;
        this.bookingService = bookingService;
        this.hotelServiceRepository = hotelServiceRepository;
    }

    /**
     * Trang chủ Khách hàng (index.html) - Hero banner, Search box, Về khách sạn, Phòng nổi bật, Dịch vụ & Đánh giá, Liên hệ
     */
    @GetMapping("/")
    public String home(Model model) {
        List<RoomType> featuredRoomTypes = roomTypeService.findAll().stream()
                .filter(rt -> rt.getActive() == null || rt.getActive())
                .limit(6)
                .collect(Collectors.toList());
        List<Review> publicReviews = reviewService.searchReviews(null, null, true).stream()
                .limit(6)
                .collect(Collectors.toList());
        ReviewStatsDTO reviewStats = reviewService.getPublicReviewStats();
        List<Amenity> amenities = amenityRepository.findAll();
        List<HotelService> hotelServices = hotelServiceRepository.findAll();

        model.addAttribute("featuredRoomTypes", featuredRoomTypes);
        model.addAttribute("reviews", publicReviews);
        model.addAttribute("reviewStats", reviewStats);
        model.addAttribute("amenities", amenities);
        model.addAttribute("hotelServices", hotelServices);

        // Default dates for search widget (tomorrow -> day after tomorrow)
        LocalDate defaultCheckIn = LocalDate.now().plusDays(1);
        LocalDate defaultCheckOut = defaultCheckIn.plusDays(1);

        model.addAttribute("defaultCheckIn", defaultCheckIn);
        model.addAttribute("defaultCheckOut", defaultCheckOut);

        return "client/index";
    }

    /**
     * Trang Danh sách Loại phòng & Bộ lọc (rooms.html)
     */
    @GetMapping("/rooms")
    public String listRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Long amenityId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        List<RoomType> allRoomTypes = roomTypeService.findAll();

        // Lọc theo các tiêu chí
        List<RoomType> filtered = allRoomTypes.stream()
                .filter(rt -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchName = rt.getName() != null && rt.getName().toLowerCase().contains(kw);
                        boolean matchDesc = rt.getDescription() != null && rt.getDescription().toLowerCase().contains(kw);
                        if (!matchName && !matchDesc) return false;
                    }
                    if (minPrice != null && rt.getPricePerNight() != null && rt.getPricePerNight().compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && rt.getPricePerNight() != null && rt.getPricePerNight().compareTo(maxPrice) > 0) {
                        return false;
                    }
                    if (guests != null && rt.getMaxGuests() != null && rt.getMaxGuests() < guests) {
                        return false;
                    }
                    if (amenityId != null && rt.getAmenities() != null) {
                        boolean matchAmenity = rt.getAmenities().stream().anyMatch(a -> a.getId().equals(amenityId));
                        if (!matchAmenity) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> {
                    if ("price_asc".equals(sortBy)) {
                        return a.getPricePerNight().compareTo(b.getPricePerNight());
                    } else if ("price_desc".equals(sortBy)) {
                        return b.getPricePerNight().compareTo(a.getPricePerNight());
                    } else if ("guests_desc".equals(sortBy)) {
                        return b.getMaxGuests().compareTo(a.getMaxGuests());
                    }
                    return a.getId().compareTo(b.getId());
                })
                .collect(Collectors.toList());

        // Pagination
        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<RoomType> paginated = (totalItems > 0 && fromIndex < totalItems)
                ? filtered.subList(fromIndex, toIndex)
                : List.of();

        boolean hasDateFilter = (checkIn != null && checkOut != null && checkOut.isAfter(checkIn));
        boolean hasFilter = (keyword != null && !keyword.trim().isEmpty())
                || minPrice != null
                || maxPrice != null
                || guests != null
                || amenityId != null
                || (sortBy != null && !sortBy.trim().isEmpty())
                || (checkIn != null || checkOut != null);
        int nights = 0;
        Map<Long, Integer> availableCountByType;

        if (hasDateFilter) {
            long nightsLong = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            nights = nightsLong > 0 ? (int) nightsLong : 1;
            LocalDateTime checkInDt = checkIn.atTime(14, 0);
            LocalDateTime checkOutDt = checkOut.atTime(12, 0);
            availableCountByType = paginated.stream().collect(Collectors.toMap(
                    RoomType::getId,
                    rt -> bookingService.findAvailableRooms(checkInDt, checkOutDt, rt.getId(), null).size()
            ));
        } else {
            // Không tự động gán ngày nếu người dùng chưa chọn -> không lọc phòng trống theo ngày ngẫu nhiên
            availableCountByType = paginated.stream().collect(Collectors.toMap(
                    RoomType::getId,
                    rt -> Boolean.TRUE.equals(rt.getActive()) ? 1 : 0
            ));
        }

        model.addAttribute("roomTypes", paginated);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("amenities", amenityRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("guests", guests);
        model.addAttribute("amenityId", amenityId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("hasFilter", hasFilter);
        model.addAttribute("hasDateFilter", hasDateFilter);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("nights", nights);
        model.addAttribute("availableCountByType", availableCountByType);
        model.addAttribute("defaultCheckIn", LocalDate.now().plusDays(1));
        model.addAttribute("defaultCheckOut", LocalDate.now().plusDays(2));

        return "client/rooms";
    }

    /**
     * Trang Chi tiết Loại phòng (room-detail.html)
     */
    @GetMapping("/rooms/{id}")
    public String roomDetail(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "2") Integer guests,
            Model model) {

        Optional<RoomType> roomTypeOpt = roomTypeService.findById(id);
        if (roomTypeOpt.isEmpty()) {
            return "redirect:/rooms";
        }

        RoomType roomType = roomTypeOpt.get();
        List<RoomType> suggestedRoomTypes = roomTypeService.findAll().stream()
                .filter(rt -> !rt.getId().equals(id))
                .limit(3)
                .toList();

        LocalDate actualIn = (checkIn != null) ? checkIn : LocalDate.now().plusDays(1);
        LocalDate actualOut = (checkOut != null && checkOut.isAfter(actualIn)) ? checkOut : actualIn.plusDays(1);

        long nightsLong = java.time.temporal.ChronoUnit.DAYS.between(actualIn, actualOut);
        int nights = nightsLong > 0 ? (int) nightsLong : 1;

        model.addAttribute("roomType", roomType);
        model.addAttribute("suggestedRoomTypes", suggestedRoomTypes);
        model.addAttribute("checkIn", actualIn);
        model.addAttribute("checkOut", actualOut);
        model.addAttribute("nights", nights);
        model.addAttribute("guests", guests);

        // Đếm số phòng trống của hạng phòng này
        LocalDateTime checkInDt = actualIn.atTime(14, 0);
        LocalDateTime checkOutDt = actualOut.atTime(12, 0);
        int availableCount = bookingService.findAvailableRooms(checkInDt, checkOutDt, roomType.getId(), null).size();
        model.addAttribute("availableCount", availableCount);

        return "client/room-detail";
    }
}
