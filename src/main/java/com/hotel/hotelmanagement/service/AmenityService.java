package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Amenity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AmenityService {
    List<Amenity> findAll();
    Page<Amenity> findAll(Pageable pageable);
    Optional<Amenity> findById(Long id);
    Amenity save(Amenity amenity);
    void toggleStatus(Long id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    
    List<Amenity> searchAmenities(String name, String status);
    Page<Amenity> searchAmenities(String name, String status, Pageable pageable);
}
