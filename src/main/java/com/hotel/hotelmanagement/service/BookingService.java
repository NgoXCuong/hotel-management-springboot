package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingService {

    List<Booking> findAll();

    Optional<Booking> findById(Long id);

    Optional<Booking> findByBookingCode(String bookingCode);

    String generateBookingCode();

    List<Room> findAvailableRooms(LocalDateTime checkIn, LocalDateTime checkOut, Long roomTypeId, Long excludeBookingId);

    Booking createBooking(Booking booking, List<Long> roomIds, String voucherCode);

    Booking save(Booking booking);

    Booking confirmBooking(Long id);

    Booking checkIn(Long id);

    Booking checkOut(Long id);

    Booking cancelBooking(Long id, String reason);

    Booking addServiceToBooking(Long bookingId, Long serviceId, Integer quantity, String note, com.hotel.hotelmanagement.entity.User createdBy);

    Booking removeServiceFromBooking(Long bookingId, Long serviceItemId);

    List<Booking> searchBookings(String keyword, Booking.BookingStatus status, Booking.BookingSource source, LocalDate checkInFrom, LocalDate checkInTo);

    org.springframework.data.domain.Page<Booking> searchBookings(String keyword, Booking.BookingStatus status, Booking.BookingSource source, LocalDate checkInFrom, LocalDate checkInTo, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Booking> findAll(org.springframework.data.domain.Pageable pageable);

    long countTotal();

    long countCheckedIn();

    long countPendingOrConfirmed();

    BigDecimal calculateTotalRevenue();
}
