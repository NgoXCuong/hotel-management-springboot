package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Room;

import java.util.List;
import java.util.Optional;

public interface RoomService {
    List<Room> findAll();
    Optional<Room> findById(Long id);
    Room save(Room room);
    void toggleStatus(Long id);
    boolean existsByRoomNumber(String roomNumber);
    boolean existsByRoomNumberAndIdNot(String roomNumber, Long id);
    
    List<Room> searchRooms(String roomNumber, Integer floor, Long roomTypeId, String status);
    org.springframework.data.domain.Page<Room> searchRooms(String roomNumber, Integer floor, Long roomTypeId, String status, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Room> findAll(org.springframework.data.domain.Pageable pageable);
    List<Integer> findDistinctFloors();
}
