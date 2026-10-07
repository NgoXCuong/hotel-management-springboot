package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity map với bảng invoices trong MySQL.
 * Mỗi booking tối đa một invoice (1-1 qua booking_id UNIQUE).
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 30)
    private String invoiceCode;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "tax", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "note")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =============== HELPER METHODS ===============

    /**
     * Tổng số tiền đã thanh toán (qua các đợt payment của booking).
     */
    public BigDecimal getPaidAmount() {
        if (booking == null || booking.getPayments() == null || booking.getPayments().isEmpty()) {
            return (booking != null && booking.getDepositAmount() != null) ? booking.getDepositAmount() : BigDecimal.ZERO;
        }
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Payment p : booking.getPayments()) {
            if (p.getPaymentStatus() == Payment.PaymentStatus.PAID && p.getAmount() != null) {
                totalPaid = totalPaid.add(p.getAmount());
            }
        }
        return totalPaid;
    }

    /**
     * Số tiền còn thiếu cần thanh toán.
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal total = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        BigDecimal paid = getPaidAmount();
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    // =============== ENUM ===============

    public enum InvoiceStatus {
        DRAFT("Bản nháp", "bg-warning-subtle text-warning border-warning-subtle", "fa-file-lines"),
        ISSUED("Đã phát hành", "bg-success-subtle text-success border-success-subtle", "fa-circle-check"),
        CANCELLED("Đã hủy", "bg-danger-subtle text-danger border-danger-subtle", "fa-ban");

        private final String displayName;
        private final String badgeClass;
        private final String icon;

        InvoiceStatus(String displayName, String badgeClass, String icon) {
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
