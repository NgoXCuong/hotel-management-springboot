package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true, length = 20)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum RoomStatus {
        AVAILABLE("Sẵn sàng", "badge bg-success-subtle text-success border border-success-subtle", "fa-door-open"),
        OCCUPIED("Đang có khách", "badge bg-primary-subtle text-primary border border-primary-subtle", "fa-user-check"),
        CLEANING("Đang dọn dẹp", "badge bg-warning-subtle text-warning border border-warning-subtle", "fa-broom"),
        MAINTENANCE("Đang bảo trì", "badge bg-danger-subtle text-danger border border-danger-subtle", "fa-screwdriver-wrench");

        private final String displayName;
        private final String badgeClass;
        private final String icon;

        RoomStatus(String displayName, String badgeClass, String icon) {
            this.displayName = displayName;
            this.badgeClass = badgeClass;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBadgeClass() {
            return badgeClass;
        }

        public String getIcon() {
            return icon;
        }
    }
}
