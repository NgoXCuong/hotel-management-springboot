package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.HotelService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface HotelServiceService {
    List<HotelService> findAll();
    Page<HotelService> findAll(Pageable pageable);
    Optional<HotelService> findById(Long id);
    HotelService save(HotelService service);
    void toggleStatus(Long id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    
    List<HotelService> searchServices(String name, String status);
    Page<HotelService> searchServices(String name, String status, Pageable pageable);
}
