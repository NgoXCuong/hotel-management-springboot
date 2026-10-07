package com.hotel.hotelmanagement.controller.payment;

import com.hotel.hotelmanagement.dto.SepayWebhookDto;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Payment;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller xử lý Webhook tự động từ cổng SePay và API Polling trạng thái thanh toán.
 */
@RestController
public class SepayWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(SepayWebhookController.class);
    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("BK-\\d{8}-[A-Za-z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALLBACK_CODE_PATTERN = Pattern.compile("BK-[A-Za-z0-9-]+", Pattern.CASE_INSENSITIVE);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public SepayWebhookController(BookingRepository bookingRepository, PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/api/sepay/webhook")
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(@RequestBody SepayWebhookDto webhookData) {
        Map<String, Object> response = new HashMap<>();

        logger.info("========== NHẬN SEPAY WEBHOOK ==========");
        logger.info("Ngân hàng: {}, STK: {}, Số tiền: {}, Nội dung: {}",
                webhookData.getGateway(),
                webhookData.getAccountNumber(),
                webhookData.getTransferAmount(),
                webhookData.getContent());

        // 1. Chỉ xử lý giao dịch tiền vào ("in")
        if (!"in".equalsIgnoreCase(webhookData.getTransferType())) {
            logger.info("Bỏ qua giao dịch không phải tiền vào: {}", webhookData.getTransferType());
            response.put("success", true);
            response.put("message", "Ignored non-in transfer type");
            return ResponseEntity.ok(response);
        }

        // 2. Trích xuất mã đặt phòng (Booking Code)
        String bookingCode = extractBookingCode(webhookData);
        if (!StringUtils.hasText(bookingCode)) {
            logger.warn("Không tìm thấy mã đặt phòng hợp lệ trong nội dung chuyển khoản: {}", webhookData.getContent());
            response.put("success", true);
            response.put("message", "No matching booking code found");
            return ResponseEntity.ok(response);
        }

        logger.info("Đã trích xuất mã đơn đặt phòng: {}", bookingCode);

        // 3. Tìm đơn đặt phòng theo mã
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode);
        if (bookingOpt.isEmpty()) {
            logger.warn("Không tìm thấy đơn đặt phòng với mã: {}", bookingCode);
            response.put("success", false);
            response.put("message", "Booking not found: " + bookingCode);
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();

        // 4. Nếu đơn đang PENDING -> Cập nhật sang CONFIRMED
        if (booking.getStatus() == Booking.BookingStatus.PENDING) {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            logger.info("Đã tự động xác nhận đơn {} thành CONFIRMED", bookingCode);
        }

        // 5. Cập nhật bản ghi Payment sang PAID
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        Payment payment;
        if (!payments.isEmpty()) {
            payment = payments.get(payments.size() - 1);
        } else {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(webhookData.getTransferAmount() != null ? webhookData.getTransferAmount() : booking.getDepositAmount())
                    .paymentMethod(Payment.PaymentMethod.BANK_TRANSFER)
                    .build();
        }

        payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        String txnCode = StringUtils.hasText(webhookData.getReferenceCode()) 
                ? webhookData.getReferenceCode().trim() 
                : "SEPAY-" + (webhookData.getId() != null ? webhookData.getId() : System.currentTimeMillis());
        payment.setTransactionCode(txnCode);
        payment.setNote("Đã thanh toán tiền cọc tự động qua SePay (MBBank). Mã GD: " + txnCode);
        paymentRepository.save(payment);

        logger.info("Thanh toán đơn {} (Mã GD: {}) đã được cập nhật PAID thành công!", bookingCode, txnCode);

        response.put("success", true);
        response.put("message", "Payment processed successfully for " + bookingCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint kiểm tra trạng thái đơn đặt phòng theo thời gian thực (cho giao diện polling tự động).
     * GET /api/booking/check-status/{bookingCode}
     */
    @GetMapping("/api/booking/check-status/{bookingCode}")
    public ResponseEntity<Map<String, Object>> checkBookingStatus(@PathVariable String bookingCode) {
        Map<String, Object> response = new HashMap<>();

        if (!StringUtils.hasText(bookingCode)) {
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode.trim());
        if (bookingOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Booking not found");
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();
        boolean isPaidOrConfirmed = booking.getStatus() == Booking.BookingStatus.CONFIRMED 
                || booking.getStatus() == Booking.BookingStatus.CHECKED_IN 
                || booking.getStatus() == Booking.BookingStatus.CHECKED_OUT;

        response.put("success", true);
        response.put("bookingCode", booking.getBookingCode());
        response.put("status", booking.getStatus().name());
        response.put("paid", isPaidOrConfirmed);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint xác nhận đã chuyển khoản cọc tức thì (phục vụ Demo & Khách bấm xác nhận trên web).
     * POST /api/booking/confirm-deposit/{bookingCode} hoặc GET
     */
    @RequestMapping(value = "/api/booking/confirm-deposit/{bookingCode}", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Map<String, Object>> confirmBookingDeposit(@PathVariable String bookingCode) {
        Map<String, Object> response = new HashMap<>();

        if (!StringUtils.hasText(bookingCode)) {
            response.put("success", false);
            response.put("message", "Mã đặt phòng không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode.trim());
        if (bookingOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn đặt phòng: " + bookingCode);
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();

        // 1. Cập nhật Booking sang CONFIRMED
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 2. Cập nhật hoặc tạo bản ghi Payment sang PAID
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        Payment payment;
        if (!payments.isEmpty()) {
            payment = payments.get(payments.size() - 1);
        } else {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getDepositAmount())
                    .paymentMethod(Payment.PaymentMethod.BANK_TRANSFER)
                    .build();
        }

        String txnCode = "MB-" + (System.currentTimeMillis() % 10000000);
        payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionCode(txnCode);
        payment.setNote("Khách xác nhận đã chuyển khoản ngân hàng (MBBank). Mã GD: " + txnCode);
        paymentRepository.save(payment);

        logger.info("Đơn {} đã được xác nhận thanh toán cọc thành công qua nút xác nhận!", bookingCode);

        response.put("success", true);
        response.put("message", "Đã xác nhận đặt cọc thành công!");
        response.put("bookingCode", booking.getBookingCode());
        return ResponseEntity.ok(response);
    }

    private String extractBookingCode(SepayWebhookDto dto) {
        if (StringUtils.hasText(dto.getCode()) && dto.getCode().trim().toUpperCase().startsWith("BK-")) {
            return dto.getCode().trim().toUpperCase();
        }

        StringBuilder searchTarget = new StringBuilder();
        if (StringUtils.hasText(dto.getContent())) {
            searchTarget.append(dto.getContent()).append(" ");
        }
        if (StringUtils.hasText(dto.getDescription())) {
            searchTarget.append(dto.getDescription());
        }

        String targetStr = searchTarget.toString();
        Matcher matcher = BOOKING_CODE_PATTERN.matcher(targetStr);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }

        Matcher fallbackMatcher = FALLBACK_CODE_PATTERN.matcher(targetStr);
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group().toUpperCase();
        }

        return null;
    }
}
