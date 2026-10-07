package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.HotelService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface HotelServiceRepository extends JpaRepository<HotelService, Long>, JpaSpecificationExecutor<HotelService> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    java.util.Optional<HotelService> findByName(String name);
}
