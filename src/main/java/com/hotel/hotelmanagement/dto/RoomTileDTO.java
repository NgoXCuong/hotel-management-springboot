package com.hotel.hotelmanagement.dto;

import com.hotel.hotelmanagement.entity.Room;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho một thẻ phòng trên Sơ đồ trực quan (Room Matrix).
 * Chứa đầy đủ thông tin phòng và thông tin khách đang ở (nếu trạng thái OCCUPIED).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTileDTO {

    private Long id;
    private String roomNumber;
    private Integer floor;
    private Room.RoomStatus status;
    private Boolean active;
    private String description;

    // Room Type info
    private Long roomTypeId;
    private String roomTypeName;
    private BigDecimal pricePerNight;
    private Integer maxGuests;

    // Active Booking info (nếu phòng đang OCCUPIED)
    private Long activeBookingId;
    private String activeBookingCode;
    private String guestName;
    private String guestPhone;
    private LocalDateTime checkIn;
    private LocalDateTime expectedCheckOut;

    // Helpers
    public boolean isAvailable() {
        return status == Room.RoomStatus.AVAILABLE;
    }

    public boolean isOccupied() {
        return status == Room.RoomStatus.OCCUPIED;
    }

    public boolean isCleaning() {
        return status == Room.RoomStatus.CLEANING;
    }

    public boolean isMaintenance() {
        return status == Room.RoomStatus.MAINTENANCE;
    }
}
