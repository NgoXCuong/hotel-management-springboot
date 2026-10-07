package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.RoomType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RoomTypeService {
    List<RoomType> findAll();
    Page<RoomType> findAll(Pageable pageable);
    Optional<RoomType> findById(Long id);
    RoomType save(RoomType roomType);
    RoomType save(RoomType roomType, List<String> newUploadedImageUrls, List<String> deletedImageUrls);
    void toggleStatus(Long id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
