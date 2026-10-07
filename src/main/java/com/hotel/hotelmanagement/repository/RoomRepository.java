package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    boolean existsByRoomNumber(String roomNumber);
    boolean existsByRoomNumberAndIdNot(String roomNumber, Long id);
    java.util.Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByStatus(Room.RoomStatus status);
    
    @Query("SELECT DISTINCT r.floor FROM Room r WHERE r.floor IS NOT NULL ORDER BY r.floor ASC")
    List<Integer> findDistinctFloors();
}
