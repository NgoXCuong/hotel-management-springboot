package com.hotel.hotelmanagement.dto;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO chứa số liệu thống kê nhanh theo trạng thái phòng phục vụ Status Legend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMatrixStatsDTO {

    private long totalRooms;
    private long availableRooms;
    private long occupiedRooms;
    private long cleaningRooms;
    private long maintenanceRooms;

    public int getOccupancyRate() {
        if (totalRooms == 0) return 0;
        return BigDecimal.valueOf(occupiedRooms)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalRooms), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
