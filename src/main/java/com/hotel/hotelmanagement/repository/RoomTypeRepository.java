package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    java.util.Optional<RoomType> findByName(String name);
}
