package com.hotel.hotelmanagement.controller.client;

import com.hotel.hotelmanagement.entity.Amenity;
import com.hotel.hotelmanagement.entity.HotelService;
import com.hotel.hotelmanagement.entity.Review;
import com.hotel.hotelmanagement.entity.RoomType;
import com.hotel.hotelmanagement.repository.AmenityRepository;
import com.hotel.hotelmanagement.repository.HotelServiceRepository;
import com.hotel.hotelmanagement.service.ReviewService;
import com.hotel.hotelmanagement.service.RoomTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PublicPageController {

    private final ReviewService reviewService;
    private final AmenityRepository amenityRepository;
    private final HotelServiceRepository hotelServiceRepository;
    private final RoomTypeService roomTypeService;

    public PublicPageController(
            ReviewService reviewService,
            AmenityRepository amenityRepository,
            HotelServiceRepository hotelServiceRepository,
            RoomTypeService roomTypeService) {
        this.reviewService = reviewService;
        this.amenityRepository = amenityRepository;
        this.hotelServiceRepository = hotelServiceRepository;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping("/about")
    public String about(Model model) {
        List<RoomType> roomTypes = roomTypeService.findAll();
        List<HotelService> services = hotelServiceRepository.findAll();
        List<Amenity> amenities = amenityRepository.findAll();

        long activeRoomTypes = roomTypes.stream().filter(rt -> Boolean.TRUE.equals(rt.getActive())).count();
        long activeServices = services.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count();
        long activeAmenities = amenities.stream().filter(a -> Boolean.TRUE.equals(a.getActive())).count();

        model.addAttribute("roomTypeCount", activeRoomTypes);
        model.addAttribute("serviceCount", activeServices);
        model.addAttribute("amenityCount", activeAmenities);
        model.addAttribute("reviewStats", reviewService.getPublicReviewStats());

        return "client/about";
    }

    @GetMapping("/services")
    public String services(Model model) {
        List<HotelService> activeServices = hotelServiceRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .toList();
        List<Amenity> activeAmenities = amenityRepository.findAllByActiveTrueOrderByNameAsc();

        model.addAttribute("services", activeServices);
        model.addAttribute("amenities", activeAmenities);

        return "client/services";
    }

    @GetMapping("/reviews")
    public String reviews(
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size,
            Model model) {

        List<Review> allVisible = reviewService.searchReviews(null, rating, true);

        if ("oldest".equals(sort)) {
            allVisible = allVisible.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .toList();
        } else if ("highest".equals(sort)) {
            allVisible = allVisible.stream()
                    .sorted((a, b) -> Integer.compare(b.getRating(), a.getRating()))
                    .toList();
        } else {
            allVisible = allVisible.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        }

        int totalItems = allVisible.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<Review> pageReviews = (totalItems > 0 && fromIndex < totalItems)
                ? allVisible.subList(fromIndex, toIndex)
                : List.of();

        model.addAttribute("reviews", pageReviews);
        model.addAttribute("reviewStats", reviewService.getPublicReviewStats());
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("ratingFilter", rating);
        model.addAttribute("sortBy", sort);

        return "client/reviews";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("hotelPhone", "1900 8888");
        model.addAttribute("hotelPhoneAlt", "(0258) 3888 999");
        model.addAttribute("hotelEmail", "reservation@grandhotel.vn");
        model.addAttribute("hotelEmailSupport", "support@grandhotel.vn");
        model.addAttribute("hotelAddress", "123 Trần Phú, Phường Lộc Thọ, TP. Nha Trang, Tỉnh Khánh Hòa");
        model.addAttribute("hotelMapEmbed", "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3899.043232810332!2d109.1967!3d12.2388!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x317067756f962d31%3A0x7d3a0429f0e1f37!2zVHLhuqduIFBow7osIE5oYSBUcmFuZywgS2jDoW5oIEjDsmE!5e0!3m2!1svi!2svn!4v1700000000000!5m2!1svi!2svn");

        return "client/contact";
    }
}
