package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.BookingServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingServiceItemRepository extends JpaRepository<BookingServiceItem, Long> {

    List<BookingServiceItem> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
