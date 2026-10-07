package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {

    List<BookingRoom> findByBookingId(Long bookingId);

    List<BookingRoom> findByRoomId(Long roomId);
}
