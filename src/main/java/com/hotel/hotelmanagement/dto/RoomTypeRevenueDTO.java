package com.hotel.hotelmanagement.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO thống kê doanh thu theo từng hạng phòng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeRevenueDTO {
    private Long roomTypeId;
    private String roomTypeName;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private int percentage;
}
