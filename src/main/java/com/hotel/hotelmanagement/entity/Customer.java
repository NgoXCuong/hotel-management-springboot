package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity map với bảng customers trong MySQL.
 * Quản lý hồ sơ khách lưu trú tại khách sạn.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tài khoản người dùng liên kết (nếu khách hàng đã đăng ký tài khoản online).
     * Có thể null đối với khách vãng lai hoặc khách do lễ tân tạo trực tiếp.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /**
     * Mã khách hàng tự động sinh dạng CUST-XXXXXX (vd: CUST-000128).
     */
    @Column(name = "customer_code", nullable = false, unique = true, length = 30)
    private String customerCode;

    /**
     * Họ và tên khách hàng (bắt buộc).
     */
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không quá 100 ký tự")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Số điện thoại liên hệ (bắt buộc, kiểm tra định dạng).
     */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
        regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$",
        message = "Số điện thoại không đúng định dạng (10 số, đầu số hợp lệ của Việt Nam)"
    )
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /**
     * Địa chỉ email liên hệ.
     */
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không quá 100 ký tự")
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Số CCCD / Hộ chiếu (định danh duy nhất).
     */
    @Size(max = 50, message = "Số CCCD/Hộ chiếu không quá 50 ký tự")
    @Column(name = "identity_number", unique = true, length = 50)
    private String identityNumber;

    /**
     * Địa chỉ liên hệ.
     */
    @Size(max = 255, message = "Địa chỉ không quá 255 ký tự")
    @Column(name = "address", length = 255)
    private String address;

    /**
     * Quốc tịch (mặc định 'Việt Nam').
     */
    @Builder.Default
    @Column(name = "nationality", length = 50)
    private String nationality = "Việt Nam";

    /**
     * Ngày tháng năm sinh.
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Giới tính: MALE, FEMALE, OTHER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    /**
     * Trạng thái hồ sơ: Đang hoạt động (true) hoặc Bị khóa (false).
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enum Giới tính khách hàng kèm hàm hiển thị tiếng Việt và biểu tượng.
     */
    public enum Gender {
        MALE("Nam", "fa-mars", "text-primary"),
        FEMALE("Nữ", "fa-venus", "text-danger"),
        OTHER("Khác", "fa-genderless", "text-secondary");

        private final String displayName;
        private final String icon;
        private final String colorClass;

        Gender(String displayName, String icon, String colorClass) {
            this.displayName = displayName;
            this.icon = icon;
            this.colorClass = colorClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public String getColorClass() {
            return colorClass;
        }
    }

    /**
     * Kiểm tra khách hàng có tài khoản hệ thống hay không.
     */
    public boolean hasUserAccount() {
        return this.user != null;
    }
}
