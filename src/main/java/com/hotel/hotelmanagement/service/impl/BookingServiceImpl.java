package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.*;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.BookingRoomRepository;
import com.hotel.hotelmanagement.repository.BookingServiceItemRepository;
import com.hotel.hotelmanagement.repository.HotelServiceRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.PromotionService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingServiceItemRepository bookingServiceItemRepository;
    private final HotelServiceRepository hotelServiceRepository;
    private final RoomRepository roomRepository;
    private final PromotionService promotionService;
    private final SecureRandom secureRandom = new SecureRandom();

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            BookingRoomRepository bookingRoomRepository,
            BookingServiceItemRepository bookingServiceItemRepository,
            HotelServiceRepository hotelServiceRepository,
            RoomRepository roomRepository,
            PromotionService promotionService) {
        this.bookingRepository = bookingRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.bookingServiceItemRepository = bookingServiceItemRepository;
        this.hotelServiceRepository = hotelServiceRepository;
        this.roomRepository = roomRepository;
        this.promotionService = promotionService;
    }

    @Override
    public List<Booking> findAll() {
        return bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public Optional<Booking> findByBookingCode(String bookingCode) {
        if (!StringUtils.hasText(bookingCode)) return Optional.empty();
        return bookingRepository.findByBookingCode(bookingCode.trim());
    }

    @Override
    public String generateBookingCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomNum = secureRandom.nextInt(10000);
            code = String.format("BK-%s-%04d", datePart, randomNum);
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }

    @Override
    public List<Room> findAvailableRooms(LocalDateTime checkIn, LocalDateTime checkOut, Long roomTypeId, Long excludeBookingId) {
        if (checkIn == null || checkOut == null) {
            checkIn = LocalDateTime.now();
            checkOut = checkIn.plusDays(1);
        }

        List<Long> bookedRoomIds = bookingRepository.findBookedRoomIdsInRange(checkIn, checkOut, excludeBookingId);

        return roomRepository.findAll().stream()
                .filter(room -> Boolean.TRUE.equals(room.getActive()))
                .filter(room -> !bookedRoomIds.contains(room.getId()))
                .filter(room -> roomTypeId == null || (room.getRoomType() != null && room.getRoomType().getId().equals(roomTypeId)))
                .sorted((r1, r2) -> r1.getRoomNumber().compareToIgnoreCase(r2.getRoomNumber()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking, List<Long> roomIds, String voucherCode) {
        // 1. Tự động sinh mã bookingCode nếu chưa có
        if (!StringUtils.hasText(booking.getBookingCode())) {
            booking.setBookingCode(generateBookingCode());
        }

        // 2. Tính số đêm lưu trú
        int numberOfNights = booking.getNumberOfNights();

        // 3. Gán phòng và tính toán tổng tiền phòng
        booking.getBookingRooms().clear();
        BigDecimal roomTotal = BigDecimal.ZERO;

        if (roomIds != null && !roomIds.isEmpty()) {
            for (Long roomId : roomIds) {
                Room room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng có ID: " + roomId));

                BigDecimal pricePerNight = (room.getRoomType() != null && room.getRoomType().getPricePerNight() != null)
                        ? room.getRoomType().getPricePerNight()
                        : BigDecimal.ZERO;

                BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(numberOfNights));
                roomTotal = roomTotal.add(subtotal);

                BookingRoom bookingRoom = BookingRoom.builder()
                        .room(room)
                        .pricePerNight(pricePerNight)
                        .numberOfNights(numberOfNights)
                        .subtotal(subtotal)
                        .build();

                booking.addBookingRoom(bookingRoom);
            }
        }

        booking.setRoomTotalAmount(roomTotal);
        if (booking.getServiceTotalAmount() == null) {
            booking.setServiceTotalAmount(BigDecimal.ZERO);
        }

        // 4. Tính Subtotal
        BigDecimal subtotalAmount = roomTotal.add(booking.getServiceTotalAmount());
        booking.setSubtotalAmount(subtotalAmount);

        // 5. Áp dụng mã Voucher khuyến mãi (nếu có)
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (StringUtils.hasText(voucherCode)) {
            Optional<Promotion> promoOpt = promotionService.findByCode(voucherCode);
            if (promoOpt.isPresent() && promotionService.checkValidity(voucherCode, subtotalAmount)) {
                Promotion promo = promoOpt.get();
                booking.setPromotion(promo);
                discountAmount = promotionService.calculateDiscount(promo, subtotalAmount);
                if (discountAmount.compareTo(subtotalAmount) > 0) {
                    discountAmount = subtotalAmount;
                }
                // Tăng số lượt dùng voucher
                promo.setUsedCount((promo.getUsedCount() != null ? promo.getUsedCount() : 0) + 1);
                promotionService.save(promo);
            } else {
                booking.setPromotion(null);
            }
        }
        booking.setDiscountAmount(discountAmount);

        // 6. Tính Tổng tiền cuối cùng và Tiền cọc
        BigDecimal totalAmount = subtotalAmount.subtract(discountAmount).max(BigDecimal.ZERO);
        booking.setTotalAmount(totalAmount);

        if (booking.getDepositAmount() == null) {
            booking.setDepositAmount(BigDecimal.ZERO);
        } else {
            booking.setDepositAmount(booking.getDepositAmount().max(BigDecimal.ZERO).min(totalAmount));
        }

        // 7. Thiết lập trạng thái mặc định nếu chưa có
        if (booking.getStatus() == null) {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
        }

        // Lưu đơn đặt phòng
        Booking savedBooking = bookingRepository.save(booking);

        // Nếu nhận phòng ngay (CHECKED_IN) -> chuyển trạng thái phòng sang OCCUPIED
        if (savedBooking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            updateAssignedRoomsStatus(savedBooking, Room.RoomStatus.OCCUPIED);
        }

        return savedBooking;
    }

    @Override
    @Transactional
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng có ID: " + id));
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking checkIn(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng có ID: " + id));
        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        booking.setActualCheckIn(LocalDateTime.now());

        // Cập nhật trạng thái các phòng gán sang OCCUPIED
        updateAssignedRoomsStatus(booking, Room.RoomStatus.OCCUPIED);

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking checkOut(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng có ID: " + id));
        booking.setStatus(Booking.BookingStatus.CHECKED_OUT);
        booking.setActualCheckOut(LocalDateTime.now());

        // Cập nhật trạng thái các phòng gán sang CLEANING
        updateAssignedRoomsStatus(booking, Room.RoomStatus.CLEANING);

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng có ID: " + id));
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);

        // Trả phòng về AVAILABLE nếu đang OCCUPIED
        updateAssignedRoomsStatus(booking, Room.RoomStatus.AVAILABLE);

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking addServiceToBooking(Long bookingId, Long serviceId, Integer quantity, String note, User createdBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng ID: " + bookingId));

        HotelService service = hotelServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ ID: " + serviceId));

        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        BigDecimal price = service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO;
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));

        BookingServiceItem item = BookingServiceItem.builder()
                .booking(booking)
                .service(service)
                .createdBy(createdBy)
                .quantity(qty)
                .pricePerUnit(price)
                .subtotal(subtotal)
                .note(note)
                .usedAt(LocalDateTime.now())
                .build();

        booking.addBookingService(item);
        booking.recalculateTotals();

        bookingServiceItemRepository.save(item);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking removeServiceFromBooking(Long bookingId, Long serviceItemId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng ID: " + bookingId));

        BookingServiceItem item = bookingServiceItemRepository.findById(serviceItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi dịch vụ ID: " + serviceItemId));

        booking.removeBookingService(item);
        bookingServiceItemRepository.delete(item);

        booking.recalculateTotals();
        return bookingRepository.save(booking);
    }

    private void updateAssignedRoomsStatus(Booking booking, Room.RoomStatus targetStatus) {
        if (booking.getBookingRooms() != null) {
            for (BookingRoom br : booking.getBookingRooms()) {
                Room room = br.getRoom();
                if (room != null) {
                    room.setStatus(targetStatus);
                    roomRepository.save(room);
                }
            }
        }
    }

    private Specification<Booking> createBookingSpec(
            String keyword,
            Booking.BookingStatus status,
            Booking.BookingSource source,
            LocalDate checkInFrom,
            LocalDate checkInTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm kiếm theo từ khóa (Mã booking, Tên khách, SĐT, Email)
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("bookingCode")), pattern);

                Join<Booking, Customer> customerJoin = root.join("customer");
                Predicate nameMatch = cb.like(cb.lower(customerJoin.get("fullName")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(customerJoin.get("phone")), pattern);
                Predicate emailMatch = cb.like(cb.lower(customerJoin.get("email")), pattern);

                predicates.add(cb.or(codeMatch, nameMatch, phoneMatch, emailMatch));
            }

            // 2. Lọc theo trạng thái
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Lọc theo nguồn đặt
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            // 4. Lọc theo khoảng ngày check-in
            if (checkInFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkIn"), checkInFrom.atStartOfDay()));
            }
            if (checkInTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkIn"), checkInTo.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<Booking> searchBookings(
            String keyword,
            Booking.BookingStatus status,
            Booking.BookingSource source,
            LocalDate checkInFrom,
            LocalDate checkInTo) {
        return bookingRepository.findAll(createBookingSpec(keyword, status, source, checkInFrom, checkInTo), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public org.springframework.data.domain.Page<Booking> searchBookings(
            String keyword,
            Booking.BookingStatus status,
            Booking.BookingSource source,
            LocalDate checkInFrom,
            LocalDate checkInTo,
            org.springframework.data.domain.Pageable pageable) {
        return bookingRepository.findAll(createBookingSpec(keyword, status, source, checkInFrom, checkInTo), pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Booking> findAll(org.springframework.data.domain.Pageable pageable) {
        return bookingRepository.findAll(pageable);
    }

    @Override
    public long countTotal() {
        return bookingRepository.count();
    }

    @Override
    public long countCheckedIn() {
        return bookingRepository.countByStatus(Booking.BookingStatus.CHECKED_IN);
    }

    @Override
    public long countPendingOrConfirmed() {
        return bookingRepository.countByStatus(Booking.BookingStatus.PENDING)
                + bookingRepository.countByStatus(Booking.BookingStatus.CONFIRMED);
    }

    @Override
    public BigDecimal calculateTotalRevenue() {
        return bookingRepository.sumTotalRevenue();
    }
}
