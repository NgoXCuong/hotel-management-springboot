package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entity map với bảng promotions trong MySQL.
 * Quản lý chương trình khuyến mãi & mã Voucher giảm giá của khách sạn.
 */
@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã Voucher độc nhất (in hoa, không dấu, không khoảng trắng, vd: SUMMER2026, VIPGUEST).
     */
    @NotBlank(message = "Mã khuyến mãi không được để trống")
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$", message = "Mã voucher chỉ gồm chữ cái in hoa, chữ số, gạch nối hoặc gạch dưới (3 - 50 ký tự)")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Tên chương trình ưu đãi.
     */
    @NotBlank(message = "Tên chương trình khuyến mãi không được để trống")
    @Size(max = 100, message = "Tên chương trình không quá 100 ký tự")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Mô tả chi tiết chương trình ưu đãi.
     */
    @Size(max = 255, message = "Mô tả không quá 255 ký tự")
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Loại giảm giá: PERCENTAGE (%) hoặc FIXED_AMOUNT (VNĐ).
     */
    @NotNull(message = "Vui lòng chọn loại giảm giá")
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /**
     * Giá trị giảm giá (% từ 1-100 hoặc số tiền VNĐ > 0).
     */
    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    /**
     * Mức giảm tối đa (VNĐ) khi áp dụng giảm theo phần trăm.
     */
    @DecimalMin(value = "0.00", message = "Mức giảm tối đa không được âm")
    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * Giá trị đơn hàng tối thiểu để áp dụng voucher (mặc định 0 VNĐ).
     */
    @NotNull(message = "Giá trị đơn hàng tối thiểu không được để trống")
    @DecimalMin(value = "0.00", message = "Giá trị đơn tối thiểu không được âm")
    @Builder.Default
    @Column(name = "min_order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /**
     * Thời gian bắt đầu áp dụng.
     */
    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * Thời gian kết thúc áp dụng.
     */
    @NotNull(message = "Thời gian kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /**
     * Giới hạn số lượt sử dụng toàn hệ thống (null = không giới hạn).
     */
    @Min(value = 1, message = "Giới hạn số lượt dùng phải từ 1 trở lên nếu được thiết lập")
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /**
     * Số lượt đã sử dụng thành công.
     */
    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    /**
     * Trạng thái kích hoạt.
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
     * Enum Loại giảm giá.
     */
    public enum DiscountType {
        PERCENTAGE("Theo phần trăm (%)", "%", "fa-percent"),
        FIXED_AMOUNT("Số tiền cố định (VNĐ)", "VNĐ", "fa-money-bill-wave");

        private final String displayName;
        private final String symbol;
        private final String icon;

        DiscountType(String displayName, String symbol, String icon) {
            this.displayName = displayName;
            this.symbol = symbol;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getIcon() {
            return icon;
        }
    }

    // ============ HELPER METHODS ============

    /**
     * Kiểm tra voucher đã hết hạn chưa.
     */
    public boolean isExpired() {
        return endDate != null && LocalDateTime.now().isAfter(endDate);
    }

    /**
     * Kiểm tra voucher chưa tới ngày áp dụng.
     */
    public boolean isUpcoming() {
        return startDate != null && LocalDateTime.now().isBefore(startDate);
    }

    /**
     * Kiểm tra voucher đã hết số lượt sử dụng chưa.
     */
    public boolean isOutOfUsage() {
        return usageLimit != null && usedCount != null && usedCount >= usageLimit;
    }

    /**
     * Kiểm tra voucher có đang khả dụng tại thời điểm này không.
     */
    public boolean isValidNow() {
        if (active == null || !active) return false;
        if (isExpired() || isUpcoming()) return false;
        return !isOutOfUsage();
    }

    /**
     * Tính phần trăm sử dụng để hiển thị thanh Progress Bar.
     */
    public int getUsagePercent() {
        if (usageLimit == null || usageLimit <= 0) return 0;
        if (usedCount == null || usedCount <= 0) return 0;
        return Math.min(100, (int) Math.round(((double) usedCount / usageLimit) * 100));
    }

    /**
     * Trả về chuỗi trạng thái chi tiết: ONGOING, EXPIRED, OUT_OF_USAGE, UPCOMING, DISABLED.
     */
    public String getStatusKey() {
        if (active == null || !active) {
            return "DISABLED";
        }
        if (isExpired()) {
            return "EXPIRED";
        }
        if (isUpcoming()) {
            return "UPCOMING";
        }
        if (isOutOfUsage()) {
            return "OUT_OF_USAGE";
        }
        return "ONGOING";
    }

    /**
     * Tính toán số tiền được giảm trừ từ đơn giá đơn hàng.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else if (discountType == DiscountType.FIXED_AMOUNT) {
            discount = discountValue;
        }

        return discount.min(orderAmount);
    }

    /**
     * Tăng số lượt sử dụng voucher.
     */
    public void incrementUsage() {
        if (this.usedCount == null) {
            this.usedCount = 1;
        } else {
            this.usedCount++;
        }
    }
}
