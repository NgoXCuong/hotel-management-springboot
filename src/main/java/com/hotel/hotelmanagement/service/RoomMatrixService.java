package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.dto.RoomMatrixStatsDTO;
import com.hotel.hotelmanagement.dto.RoomTileDTO;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Room;
import com.hotel.hotelmanagement.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RoomMatrixService {

    /**
     * Lấy sơ đồ phòng gom nhóm theo Tầng kèm bộ lọc (bao gồm tìm kiếm theo số phòng)
     */
    Map<Integer, List<RoomTileDTO>> getRoomMatrixByFloor(String roomNumber, Integer floor, Long roomTypeId, Room.RoomStatus status);

    /**
     * Lấy sơ đồ phòng gom nhóm theo Tầng kèm bộ lọc
     */
    Map<Integer, List<RoomTileDTO>> getRoomMatrixByFloor(Integer floor, Long roomTypeId, Room.RoomStatus status);

    /**
     * Thống kê số lượng phòng theo từng trạng thái
     */
    RoomMatrixStatsDTO getRoomMatrixStats();

    /**
     * Cập nhật nhanh trạng thái phòng (ví dụ: CLEANING -> AVAILABLE)
     */
    void updateRoomStatus(Long roomId, Room.RoomStatus newStatus);

    /**
     * Đặt phòng nhanh trực tiếp từ ô phòng trống trên sơ đồ
     */
    Booking quickBookRoom(
            Long roomId,
            String guestName,
            String guestPhone,
            String guestEmail,
            String guestIdentity,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            Integer guests,
            BigDecimal deposit,
            User createdBy
    );
}
