package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity map với bảng booking_rooms trong MySQL.
 * Chi tiết các phòng được gán cho một đơn đặt phòng.
 */
@Entity
@Table(name = "booking_rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"booking_id", "room_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "price_per_night", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "number_of_nights", nullable = false)
    private Integer numberOfNights;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
