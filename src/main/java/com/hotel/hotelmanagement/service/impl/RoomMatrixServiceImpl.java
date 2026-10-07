package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.dto.RoomMatrixStatsDTO;
import com.hotel.hotelmanagement.dto.RoomTileDTO;
import com.hotel.hotelmanagement.entity.*;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.BookingRoomRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.CustomerService;
import com.hotel.hotelmanagement.service.RoomMatrixService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomMatrixServiceImpl implements RoomMatrixService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final BookingService bookingService;

    public RoomMatrixServiceImpl(
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            BookingRoomRepository bookingRoomRepository,
            CustomerRepository customerRepository,
            CustomerService customerService,
            BookingService bookingService) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.bookingService = bookingService;
    }

    @Override
    public Map<Integer, List<RoomTileDTO>> getRoomMatrixByFloor(Integer floor, Long roomTypeId, Room.RoomStatus status) {
        return getRoomMatrixByFloor(null, floor, roomTypeId, status);
    }

    @Override
    public Map<Integer, List<RoomTileDTO>> getRoomMatrixByFloor(String roomNumber, Integer floor, Long roomTypeId, Room.RoomStatus status) {
        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(roomNumber)) {
                predicates.add(cb.like(cb.lower(root.get("roomNumber")), "%" + roomNumber.trim().toLowerCase() + "%"));
            }
            if (floor != null) {
                predicates.add(cb.equal(root.get("floor"), floor));
            }
            if (roomTypeId != null) {
                predicates.add(cb.equal(root.get("roomType").get("id"), roomTypeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Room> rooms = roomRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "floor").and(Sort.by(Sort.Direction.ASC, "roomNumber")));

        // Map sang RoomTileDTO và nhóm theo Tầng
        Map<Integer, List<RoomTileDTO>> matrixMap = new TreeMap<>();

        for (Room r : rooms) {
            int floorNum = (r.getFloor() != null) ? r.getFloor() : 1;

            RoomTileDTO dto = RoomTileDTO.builder()
                    .id(r.getId())
                    .roomNumber(r.getRoomNumber())
                    .floor(r.getFloor())
                    .status(r.getStatus())
                    .active(r.getActive())
                    .description(r.getDescription())
                    .roomTypeId(r.getRoomType() != null ? r.getRoomType().getId() : null)
                    .roomTypeName(r.getRoomType() != null ? r.getRoomType().getName() : "Tiêu chuẩn")
                    .pricePerNight(r.getRoomType() != null ? r.getRoomType().getPricePerNight() : BigDecimal.ZERO)
                    .maxGuests(r.getRoomType() != null ? r.getRoomType().getMaxGuests() : 2)
                    .build();

            // Nếu phòng đang có khách (OCCUPIED), tìm đơn đặt phòng active
            if (r.getStatus() == Room.RoomStatus.OCCUPIED) {
                List<Booking> activeBookings = bookingRepository.findActiveCheckedInBookingByRoomId(r.getId());
                if (!activeBookings.isEmpty()) {
                    Booking b = activeBookings.get(0);
                    dto.setActiveBookingId(b.getId());
                    dto.setActiveBookingCode(b.getBookingCode());
                    if (b.getCustomer() != null) {
                        dto.setGuestName(b.getCustomer().getFullName());
                        dto.setGuestPhone(b.getCustomer().getPhone());
                    }
                    dto.setCheckIn(b.getCheckIn());
                    dto.setExpectedCheckOut(b.getCheckOut());
                }
            }

            matrixMap.computeIfAbsent(floorNum, k -> new ArrayList<>()).add(dto);
        }

        return matrixMap;
    }

    @Override
    public RoomMatrixStatsDTO getRoomMatrixStats() {
        List<Room> all = roomRepository.findAll();
        long total = all.size();
        long available = all.stream().filter(r -> r.getStatus() == Room.RoomStatus.AVAILABLE).count();
        long occupied = all.stream().filter(r -> r.getStatus() == Room.RoomStatus.OCCUPIED).count();
        long cleaning = all.stream().filter(r -> r.getStatus() == Room.RoomStatus.CLEANING).count();
        long maintenance = all.stream().filter(r -> r.getStatus() == Room.RoomStatus.MAINTENANCE).count();

        return RoomMatrixStatsDTO.builder()
                .totalRooms(total)
                .availableRooms(available)
                .occupiedRooms(occupied)
                .cleaningRooms(cleaning)
                .maintenanceRooms(maintenance)
                .build();
    }

    @Override
    @Transactional
    public void updateRoomStatus(Long roomId, Room.RoomStatus newStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ID: " + roomId));
        room.setStatus(newStatus);
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public Booking quickBookRoom(
            Long roomId,
            String guestName,
            String guestPhone,
            String guestEmail,
            String guestIdentity,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            Integer guests,
            BigDecimal deposit,
            User createdBy) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ID: " + roomId));

        // 1. Tìm hoặc tạo mới khách hàng
        Customer customer;
        if (StringUtils.hasText(guestPhone)) {
            Optional<Customer> existingOpt = customerRepository.findFirstByPhone(guestPhone.trim());
            if (existingOpt.isPresent()) {
                customer = existingOpt.get();
                if (StringUtils.hasText(guestName)) {
                    customer.setFullName(guestName.trim());
                }
                if (StringUtils.hasText(guestEmail)) {
                    customer.setEmail(guestEmail.trim().toLowerCase());
                }
                if (StringUtils.hasText(guestIdentity)) {
                    customer.setIdentityNumber(guestIdentity.trim());
                }
                customer = customerService.save(customer);
            } else {
                customer = Customer.builder()
                        .fullName(StringUtils.hasText(guestName) ? guestName.trim() : "Khách vãng lai")
                        .phone(guestPhone.trim())
                        .email(StringUtils.hasText(guestEmail) ? guestEmail.trim() : null)
                        .identityNumber(StringUtils.hasText(guestIdentity) ? guestIdentity.trim() : null)
                        .customerCode(customerService.generateCustomerCode())
                        .nationality("Việt Nam")
                        .active(true)
                        .build();
                customer = customerService.save(customer);
            }
        } else {
            // Trường hợp không nhập số điện thoại
            customer = Customer.builder()
                    .fullName(StringUtils.hasText(guestName) ? guestName.trim() : "Khách vãng lai")
                    .phone("09" + String.format("%08d", (int) (Math.random() * 100000000)))
                    .customerCode(customerService.generateCustomerCode())
                    .nationality("Việt Nam")
                    .active(true)
                    .build();
            customer = customerService.save(customer);
        }

        // 2. Tính thời gian & tiền phòng
        LocalDateTime actualIn = (checkIn != null) ? checkIn : LocalDateTime.now();
        LocalDateTime actualOut = (checkOut != null && checkOut.isAfter(actualIn)) ? checkOut : actualIn.plusDays(1).withHour(12).withMinute(0);
        long nightsLong = ChronoUnit.DAYS.between(actualIn.toLocalDate(), actualOut.toLocalDate());
        int nights = nightsLong > 0 ? (int) nightsLong : 1;

        BigDecimal price = (room.getRoomType() != null && room.getRoomType().getPricePerNight() != null)
                ? room.getRoomType().getPricePerNight()
                : BigDecimal.ZERO;
        BigDecimal roomTotal = price.multiply(BigDecimal.valueOf(nights));
        BigDecimal dep = (deposit != null && deposit.compareTo(BigDecimal.ZERO) >= 0) ? deposit.min(roomTotal) : BigDecimal.ZERO;

        // 3. Tạo Booking và nhận phòng ngay (CHECKED_IN)
        Booking booking = Booking.builder()
                .bookingCode(bookingService.generateBookingCode())
                .customer(customer)
                .createdBy(createdBy)
                .numberOfGuests(guests != null && guests > 0 ? guests : 1)
                .source(Booking.BookingSource.WALK_IN)
                .checkIn(actualIn)
                .checkOut(actualOut)
                .actualCheckIn(actualIn)
                .status(Booking.BookingStatus.CHECKED_IN)
                .roomTotalAmount(roomTotal)
                .serviceTotalAmount(BigDecimal.ZERO)
                .subtotalAmount(roomTotal)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(roomTotal)
                .depositAmount(dep)
                .note("Đặt phòng nhanh trực tiếp từ Sơ đồ phòng")
                .build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .booking(booking)
                .room(room)
                .pricePerNight(price)
                .numberOfNights(nights)
                .subtotal(roomTotal)
                .build();

        booking.addBookingRoom(bookingRoom);

        // 4. Đổi trạng thái phòng sang OCCUPIED
        room.setStatus(Room.RoomStatus.OCCUPIED);
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }
}
