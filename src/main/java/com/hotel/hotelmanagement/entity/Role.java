package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity map với bảng roles trong MySQL.
 * Lưu trữ các vai trò: ROLE_ADMIN, ROLE_MANAGER, ROLE_RECEPTIONIST, ROLE_CUSTOMER.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    public String getDisplayName() {
        if (name == null) return "";
        return switch (name) {
            case "ROLE_ADMIN" -> "Quản trị viên";
            case "ROLE_MANAGER" -> "Quản lý khách sạn";
            case "ROLE_RECEPTIONIST" -> "Lễ tân";
            case "ROLE_CUSTOMER" -> "Khách hàng";
            default -> name.replace("ROLE_", "");
        };
    }
}
