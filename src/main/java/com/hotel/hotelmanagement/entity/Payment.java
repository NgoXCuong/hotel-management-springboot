package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity map với bảng payments trong MySQL.
 * Một booking có thể có nhiều payment (đặt cọc, thanh toán từng phần, thanh toán cuối...).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transaction_code", unique = true, length = 100)
    private String transactionCode;

    @Column(name = "gateway_response", columnDefinition = "JSON")
    private String gatewayResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "note")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =============== ENUMS ===============

    public enum PaymentMethod {
        CASH("Tiền mặt", "fa-money-bill-wave", "text-success"),
        BANK_TRANSFER("Chuyển khoản", "fa-building-columns", "text-primary"),
        CARD("Thẻ tín dụng/ghi nợ", "fa-credit-card", "text-info"),
        MOMO("Ví MoMo", "fa-wallet", "text-danger"),
        VNPAY("Cổng VNPAY", "fa-qrcode", "text-warning");

        private final String displayName;
        private final String icon;
        private final String colorClass;

        PaymentMethod(String displayName, String icon, String colorClass) {
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

    public enum PaymentStatus {
        PENDING("Chờ thanh toán", "bg-warning-subtle text-warning border-warning-subtle"),
        PAID("Đã thanh toán", "bg-success-subtle text-success border-success-subtle"),
        FAILED("Thất bại", "bg-danger-subtle text-danger border-danger-subtle"),
        REFUNDED("Đã hoàn tiền", "bg-secondary-subtle text-secondary border-secondary-subtle");

        private final String displayName;
        private final String badgeClass;

        PaymentStatus(String displayName, String badgeClass) {
            this.displayName = displayName;
            this.badgeClass = badgeClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }
}
