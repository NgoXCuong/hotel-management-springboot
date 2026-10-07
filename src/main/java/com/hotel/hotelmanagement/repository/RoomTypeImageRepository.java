package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.RoomTypeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {
}
