package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity map với bảng bookings trong MySQL.
 * Quản lý thông tin đơn đặt phòng lưu trú của khách sạn.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã đơn đặt phòng unique dạng BK-YYYYMMDD-XXXX (vd: BK-20260415-089).
     */
    @Column(name = "booking_code", nullable = false, unique = true, length = 30)
    private String bookingCode;

    /**
     * Khách hàng đại diện đặt phòng (bắt buộc).
     */
    @NotNull(message = "Vui lòng chọn khách hàng")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Nhân viên tạo đơn (null nếu khách tự đặt online).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Mã khuyến mãi / Voucher áp dụng (nếu có).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    /**
     * Số lượng khách lưu trú.
     */
    @NotNull(message = "Số lượng khách không được để trống")
    @Min(value = 1, message = "Số lượng khách phải từ 1 người trở lên")
    @Builder.Default
    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests = 1;

    /**
     * Nguồn đặt phòng: WALK_IN (tại quầy), PHONE, ONLINE, OTA.
     */
    @NotNull(message = "Vui lòng chọn nguồn đặt phòng")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "source", nullable = false, length = 20)
    private BookingSource source = BookingSource.ONLINE;

    /**
     * Thời gian Check-in dự kiến.
     */
    @NotNull(message = "Thời gian nhận phòng không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    /**
     * Thời gian Check-out dự kiến.
     */
    @NotNull(message = "Thời gian trả phòng không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "check_out", nullable = false)
    private LocalDateTime checkOut;

    /**
     * Thời gian thực tế khách nhận phòng.
     */
    @Column(name = "actual_check_in")
    private LocalDateTime actualCheckIn;

    /**
     * Thời gian thực tế khách trả phòng.
     */
    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    /**
     * Trạng thái đơn đặt phòng: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * Tổng tiền phòng (tính theo số đêm và đơn giá từng phòng).
     */
    @Builder.Default
    @Column(name = "room_total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal roomTotalAmount = BigDecimal.ZERO;

    /**
     * Tổng tiền dịch vụ bổ sung.
     */
    @Builder.Default
    @Column(name = "service_total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceTotalAmount = BigDecimal.ZERO;

    /**
     * Tạm tính trước giảm giá (roomTotalAmount + serviceTotalAmount).
     */
    @Builder.Default
    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    /**
     * Số tiền được giảm giá (từ Promotion).
     */
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Tổng tiền thanh toán cuối cùng (subtotalAmount - discountAmount).
     */
    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Số tiền khách đã đặt cọc trước.
     */
    @Builder.Default
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    /**
     * Lý do hủy đơn (nếu status = CANCELLED).
     */
    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    /**
     * Thời điểm hủy đơn.
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Ghi chú lưu trú của khách hàng / lễ tân.
     */
    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Danh sách các phòng gán cho đơn đặt này.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<BookingRoom> bookingRooms = new ArrayList<>();

    /**
     * Danh sách các dịch vụ bổ sung đã sử dụng.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<BookingServiceItem> bookingServices = new ArrayList<>();

    /**
     * Lịch sử các đợt thanh toán / đặt cọc của booking.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    // ============ ENUMS ============

    public enum BookingStatus {
        PENDING("Chờ duyệt", "bg-warning-subtle text-warning border-warning-subtle", "fa-clock"),
        CONFIRMED("Đã duyệt", "bg-primary-subtle text-primary border-primary-subtle", "fa-circle-check"),
        CHECKED_IN("Đang lưu trú", "bg-success-subtle text-success border-success-subtle", "fa-door-open"),
        CHECKED_OUT("Đã trả phòng", "bg-secondary-subtle text-secondary border-secondary-subtle", "fa-check-double"),
        CANCELLED("Đã hủy", "bg-danger-subtle text-danger border-danger-subtle", "fa-ban"),
        NO_SHOW("Vắng mặt", "bg-dark-subtle text-dark border-dark-subtle", "fa-user-xmark");

        private final String displayName;
        private final String badgeClass;
        private final String icon;

        BookingStatus(String displayName, String badgeClass, String icon) {
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

    public enum BookingSource {
        WALK_IN("Khách tại quầy", "fa-person-walking", "text-warning"),
        PHONE("Đặt qua điện thoại", "fa-phone", "text-info"),
        ONLINE("Website trực tuyến", "fa-globe", "text-primary"),
        OTA("Đại lý du lịch (OTA)", "fa-plane-departure", "text-success");

        private final String displayName;
        private final String icon;
        private final String colorClass;

        BookingSource(String displayName, String icon, String colorClass) {
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

    // ============ HELPER METHODS ============

    /**
     * Tính số đêm lưu trú (tối thiểu 1 đêm).
     */
    public int getNumberOfNights() {
        if (checkIn == null || checkOut == null) return 1;
        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        return nights > 0 ? (int) nights : 1;
    }

    /**
     * Số tiền còn lại cần thanh toán (Tổng tiền - Tiền cọc).
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal deposit = depositAmount != null ? depositAmount : BigDecimal.ZERO;
        return total.subtract(deposit).max(BigDecimal.ZERO);
    }

    /**
     * Lấy chuỗi danh sách số phòng, ví dụ: "P.101, P.102".
     */
    public String getRoomNumbersString() {
        if (bookingRooms == null || bookingRooms.isEmpty()) {
            return "Chưa gán phòng";
        }
        return bookingRooms.stream()
                .map(br -> br.getRoom() != null ? "P." + br.getRoom().getRoomNumber() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    /**
     * Thêm phòng vào đơn đặt.
     */
    public void addBookingRoom(BookingRoom bookingRoom) {
        if (bookingRooms == null) {
            bookingRooms = new ArrayList<>();
        }
        bookingRooms.add(bookingRoom);
        bookingRoom.setBooking(this);
    }

    /**
     * Thêm dịch vụ vào đơn đặt.
     */
    public void addBookingService(BookingServiceItem serviceItem) {
        if (bookingServices == null) {
            bookingServices = new ArrayList<>();
        }
        bookingServices.add(serviceItem);
        serviceItem.setBooking(this);
    }

    /**
     * Xóa dịch vụ khỏi đơn đặt.
     */
    public void removeBookingService(BookingServiceItem serviceItem) {
        if (bookingServices != null) {
            bookingServices.remove(serviceItem);
            serviceItem.setBooking(null);
        }
    }

    /**
     * Thêm đợt thanh toán vào đơn đặt.
     */
    public void addPayment(Payment payment) {
        if (payments == null) {
            payments = new ArrayList<>();
        }
        payments.add(payment);
        payment.setBooking(this);
    }

    /**
     * Tự động tính toán lại tổng tiền dịch vụ và tổng tiền cần thanh toán.
     */
    public void recalculateTotals() {
        // 1. Tính tổng tiền dịch vụ
        BigDecimal sTotal = BigDecimal.ZERO;
        if (bookingServices != null) {
            for (BookingServiceItem item : bookingServices) {
                if (item.getSubtotal() != null) {
                    sTotal = sTotal.add(item.getSubtotal());
                }
            }
        }
        this.serviceTotalAmount = sTotal;

        // 2. Tính subtotal = roomTotalAmount + serviceTotalAmount
        BigDecimal rTotal = this.roomTotalAmount != null ? this.roomTotalAmount : BigDecimal.ZERO;
        this.subtotalAmount = rTotal.add(sTotal);

        // 3. Giảm giá voucher không vượt quá subtotal
        BigDecimal disc = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO;
        if (disc.compareTo(this.subtotalAmount) > 0) {
            disc = this.subtotalAmount;
            this.discountAmount = disc;
        }

        // 4. Tổng thanh toán
        this.totalAmount = this.subtotalAmount.subtract(disc).max(BigDecimal.ZERO);

        // 5. Cọc không vượt quá totalAmount
        if (this.depositAmount == null) {
            this.depositAmount = BigDecimal.ZERO;
        } else {
            this.depositAmount = this.depositAmount.max(BigDecimal.ZERO).min(this.totalAmount);
        }
    }
}
