package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByBookingCode(String bookingCode);

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByStatus(Booking.BookingStatus status);

    @Query("SELECT DISTINCT br.room.id FROM BookingRoom br " +
           "WHERE br.booking.status NOT IN ('CANCELLED') " +
           "AND br.booking.checkIn < :checkOut AND br.booking.checkOut > :checkIn " +
           "AND (:excludeBookingId IS NULL OR br.booking.id != :excludeBookingId)")
    List<Long> findBookedRoomIdsInRange(
            @Param("checkIn") LocalDateTime checkIn,
            @Param("checkOut") LocalDateTime checkOut,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("SELECT DISTINCT br.booking FROM BookingRoom br " +
           "WHERE br.room.id = :roomId " +
           "AND br.booking.status = 'CHECKED_IN' " +
           "ORDER BY br.booking.createdAt DESC")
    List<Booking> findActiveCheckedInBookingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status NOT IN ('CANCELLED')")
    BigDecimal sumTotalRevenue();
}
