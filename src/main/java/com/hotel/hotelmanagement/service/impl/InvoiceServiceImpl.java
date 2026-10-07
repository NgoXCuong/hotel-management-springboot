package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.*;
import com.hotel.hotelmanagement.repository.*;
import com.hotel.hotelmanagement.service.InvoiceService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Optional<Invoice> findById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Override
    public Optional<Invoice> findByInvoiceCode(String invoiceCode) {
        return invoiceRepository.findByInvoiceCode(invoiceCode);
    }

    @Override
    public Optional<Invoice> findByBookingId(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId);
    }

    /**
     * Tự động sinh mã hóa đơn: INV-YYYYMMDD-XXXX
     */
    @Override
    public String generateInvoiceCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomNum = 1000 + secureRandom.nextInt(9000);
            code = "INV-" + datePart + "-" + randomNum;
        } while (invoiceRepository.existsByInvoiceCode(code));
        return code;
    }

    /**
     * Khởi tạo hoặc lấy bản nháp hóa đơn từ đơn đặt phòng
     */
    @Override
    @Transactional
    public Invoice createOrGetDraftInvoice(Long bookingId, BigDecimal vatRate, User issuedBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng ID: " + bookingId));

        BigDecimal rate = (vatRate != null && vatRate.compareTo(BigDecimal.ZERO) >= 0) ? vatRate : new BigDecimal("8.00");

        Optional<Invoice> existingOpt = invoiceRepository.findByBookingId(bookingId);
        if (existingOpt.isPresent()) {
            Invoice inv = existingOpt.get();
            // Nếu là DRAFT thì cập nhật lại số liệu tài chính mới nhất
            if (inv.getStatus() == Invoice.InvoiceStatus.DRAFT) {
                calculateInvoiceTotals(inv, booking, rate);
                if (issuedBy != null) inv.setIssuedBy(issuedBy);
                return invoiceRepository.save(inv);
            }
            return inv;
        }

        // Tạo mới hóa đơn bản nháp
        Invoice invoice = Invoice.builder()
                .invoiceCode(generateInvoiceCode())
                .booking(booking)
                .issuedBy(issuedBy)
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();

        calculateInvoiceTotals(invoice, booking, rate);
        return invoiceRepository.save(invoice);
    }

    private void calculateInvoiceTotals(Invoice invoice, Booking booking, BigDecimal vatRate) {
        BigDecimal subtotal = booking.getSubtotalAmount() != null ? booking.getSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        // Phương án 1: Giá phòng niêm yết đã bao gồm thuế VAT (Giá Net)
        // Tổng tiền thanh toán đúng bằng (Tiền phòng + Tiền dịch vụ - Giảm giá), không bị đội giá
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        // Bóc tách tiền thuế VAT đã nằm bên trong tổng tiền theo quy định thuế: Tax = Total * vatRate / (100 + vatRate)
        BigDecimal tax = BigDecimal.ZERO;
        if (vatRate != null && vatRate.compareTo(BigDecimal.ZERO) > 0 && total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = new BigDecimal("100").add(vatRate);
            tax = total.multiply(vatRate).divide(divisor, 0, RoundingMode.HALF_UP);
        }

        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTax(tax);
        invoice.setTotalAmount(total);
    }

    /**
     * Ghi nhận thanh toán và phát hành hóa đơn (Status = ISSUED, Booking = CHECKED_OUT)
     */
    @Override
    @Transactional
    public Invoice processPaymentAndIssue(
            Long invoiceId,
            Payment.PaymentMethod paymentMethod,
            BigDecimal paymentAmount,
            String transactionCode,
            String note,
            User cashier) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn ID: " + invoiceId));

        Booking booking = invoice.getBooking();

        // 1. Ghi nhận giao dịch thanh toán (nếu có số tiền thanh toán > 0)
        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            String trxCode = StringUtils.hasText(transactionCode)
                    ? transactionCode.trim()
                    : "PAY-" + System.currentTimeMillis() % 100000000;

            Payment payment = Payment.builder()
                    .booking(booking)
                    .processedBy(cashier)
                    .amount(paymentAmount)
                    .paymentMethod(paymentMethod != null ? paymentMethod : Payment.PaymentMethod.CASH)
                    .paymentStatus(Payment.PaymentStatus.PAID)
                    .transactionCode(trxCode)
                    .paidAt(LocalDateTime.now())
                    .note(note)
                    .build();

            booking.addPayment(payment);
            paymentRepository.save(payment);
        }

        // 2. Chuyển trạng thái Hóa đơn sang ISSUED
        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        if (cashier != null) {
            invoice.setIssuedBy(cashier);
        }

        // 3. Hoàn tất Booking: Đổi trạng thái Booking sang CHECKED_OUT và chuyển phòng sang CLEANING
        if (booking.getStatus() != Booking.BookingStatus.CHECKED_OUT) {
            booking.setStatus(Booking.BookingStatus.CHECKED_OUT);
            if (booking.getActualCheckOut() == null) {
                booking.setActualCheckOut(LocalDateTime.now());
            }

            if (booking.getBookingRooms() != null) {
                for (BookingRoom br : booking.getBookingRooms()) {
                    Room room = br.getRoom();
                    if (room != null) {
                        room.setStatus(Room.RoomStatus.CLEANING);
                        roomRepository.save(room);
                    }
                }
            }
        }

        bookingRepository.save(booking);
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public Invoice cancelInvoice(Long id, String reason) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn ID: " + id));

        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        if (StringUtils.hasText(reason)) {
            invoice.setNote(StringUtils.hasText(invoice.getNote()) ? invoice.getNote() + " | Lý do hủy: " + reason : "Lý do hủy: " + reason);
        }
        return invoiceRepository.save(invoice);
    }

    private Specification<Invoice> createInvoiceSpec(String keyword, Invoice.InvoiceStatus status, LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                Join<Invoice, Booking> bookingJoin = root.join("booking");
                Join<Booking, Customer> customerJoin = bookingJoin.join("customer");

                Predicate invoiceCodePred = cb.like(cb.lower(root.get("invoiceCode")), term);
                Predicate bookingCodePred = cb.like(cb.lower(bookingJoin.get("bookingCode")), term);
                Predicate customerNamePred = cb.like(cb.lower(customerJoin.get("fullName")), term);
                Predicate customerPhonePred = cb.like(cb.lower(customerJoin.get("phone")), term);

                predicates.add(cb.or(invoiceCodePred, bookingCodePred, customerNamePred, customerPhonePred));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<Invoice> searchInvoices(String keyword, Invoice.InvoiceStatus status, LocalDate fromDate, LocalDate toDate) {
        return invoiceRepository.findAll(createInvoiceSpec(keyword, status, fromDate, toDate), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public org.springframework.data.domain.Page<Invoice> searchInvoices(String keyword, Invoice.InvoiceStatus status, LocalDate fromDate, LocalDate toDate, org.springframework.data.domain.Pageable pageable) {
        return invoiceRepository.findAll(createInvoiceSpec(keyword, status, fromDate, toDate), pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Invoice> findAll(org.springframework.data.domain.Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Override
    public long countTotal() {
        return invoiceRepository.count();
    }

    @Override
    public long countIssued() {
        return invoiceRepository.countByStatus(Invoice.InvoiceStatus.ISSUED);
    }

    @Override
    public long countDraft() {
        return invoiceRepository.countByStatus(Invoice.InvoiceStatus.DRAFT);
    }

    @Override
    public long countCancelled() {
        return invoiceRepository.countByStatus(Invoice.InvoiceStatus.CANCELLED);
    }

    @Override
    public BigDecimal calculateTotalRevenue() {
        BigDecimal rev = invoiceRepository.sumTotalRevenue();
        return rev != null ? rev : BigDecimal.ZERO;
    }
}
