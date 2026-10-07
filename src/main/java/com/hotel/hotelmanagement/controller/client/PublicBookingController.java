package com.hotel.hotelmanagement.controller.client;

import com.hotel.hotelmanagement.entity.*;
import com.hotel.hotelmanagement.repository.*;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.RoomTypeService;
import com.hotel.hotelmanagement.service.payment.MomoService;
import com.hotel.hotelmanagement.service.payment.VietQrService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller xử lý quy trình đặt phòng khách hàng trực tuyến:
 * - Bước 1: /booking/checkout (Form nhập thông tin + tóm tắt + phương thức thanh toán)
 * - Tích hợp VietQR (Ngân hàng): Tự động tạo mã QR VietQR chuẩn kèm số tiền và mã đơn
 * - Tích hợp Ví MoMo: Tạo mã QR thanh toán MoMo
 * - Bước 2: /booking/result (Màn hình kết quả & hóa đơn xác nhận)
 */
@Controller
public class PublicBookingController {

    private final RoomTypeService roomTypeService;
    private final BookingService bookingService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentRepository paymentRepository;
    private final VietQrService vietQrService;
    private final MomoService momoService;

    public PublicBookingController(
            RoomTypeService roomTypeService,
            BookingService bookingService,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            PromotionRepository promotionRepository,
            PaymentRepository paymentRepository,
            VietQrService vietQrService,
            MomoService momoService) {
        this.roomTypeService = roomTypeService;
        this.bookingService = bookingService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.promotionRepository = promotionRepository;
        this.paymentRepository = paymentRepository;
        this.vietQrService = vietQrService;
        this.momoService = momoService;
    }

    /**
     * Màn hình Thanh toán & Xác nhận đơn (checkout.html)
     */
    @GetMapping("/booking/checkout")
    public String showCheckout(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "2") Integer guests,
            @RequestParam(required = false) String promoCode,
            Authentication authentication,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (roomTypeId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn hạng phòng trước khi đặt!");
            return "redirect:/rooms";
        }

        Optional<RoomType> roomTypeOpt = roomTypeService.findById(roomTypeId);
        if (roomTypeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy loại phòng!");
            return "redirect:/rooms";
        }

        RoomType roomType = roomTypeOpt.get();
        LocalDate in = (checkIn != null) ? checkIn : LocalDate.now().plusDays(1);
        LocalDate out = (checkOut != null && checkOut.isAfter(in)) ? checkOut : in.plusDays(1);
        long nightsLong = ChronoUnit.DAYS.between(in, out);
        int nights = nightsLong > 0 ? (int) nightsLong : 1;
        int guestCount = (guests != null && guests > 0) ? guests : 2;

        BigDecimal pricePerNight = roomType.getPricePerNight() != null ? roomType.getPricePerNight() : BigDecimal.ZERO;
        BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights));

        // Kiểm tra mã voucher nếu có
        BigDecimal discount = BigDecimal.ZERO;
        Promotion appliedPromo = null;
        if (StringUtils.hasText(promoCode)) {
            Optional<Promotion> promoOpt = promotionRepository.findByCode(promoCode.trim().toUpperCase());
            if (promoOpt.isPresent() && promoOpt.get().isValidNow()) {
                Promotion p = promoOpt.get();
                if (p.getMinOrderAmount() == null || subtotal.compareTo(p.getMinOrderAmount()) >= 0) {
                    appliedPromo = p;
                    discount = p.calculateDiscount(subtotal);
                }
            }
        }
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal deposit = total.multiply(new BigDecimal("0.3")).setScale(0, java.math.RoundingMode.HALF_UP);

        // Pre-fill user details if logged in
        Customer loggedCustomer = null;
        if (authentication != null) {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                loggedCustomer = customerRepository.findByUserId(u.getId())
                        .orElse(customerRepository.findFirstByEmail(u.getEmail()).orElse(null));
                if (loggedCustomer == null) {
                    loggedCustomer = Customer.builder()
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .phone(u.getPhone())
                            .build();
                }
            }
        }

        model.addAttribute("roomType", roomType);
        model.addAttribute("checkIn", in);
        model.addAttribute("checkOut", out);
        model.addAttribute("nights", nights);
        model.addAttribute("guests", guestCount);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total);
        model.addAttribute("deposit", deposit);
        model.addAttribute("promoCode", promoCode);
        model.addAttribute("appliedPromo", appliedPromo);
        model.addAttribute("loggedCustomer", loggedCustomer);

        request.getSession(true);

        return "client/checkout";
    }

    /**
     * Xử lý Đặt phòng trực tuyến (POST /booking/checkout)
     */
    @PostMapping("/booking/checkout")
    public String processCheckout(
            @RequestParam Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "2") Integer guests,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String identityNumber,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String promoCode,
            @RequestParam(defaultValue = "BANK_TRANSFER") Payment.PaymentMethod paymentMethod,
            HttpServletRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            LocalDateTime checkInDt = checkIn.atTime(14, 0);
            LocalDateTime checkOutDt = checkOut.atTime(12, 0);

            // 1. Kiểm tra phòng trống thuộc RoomType này
            List<Room> availableRooms = bookingService.findAvailableRooms(checkInDt, checkOutDt, roomTypeId, null);
            if (availableRooms.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rất tiếc, hạng phòng này đã hết phòng trống trong khoảng thời gian đã chọn!");
                return "redirect:/booking/checkout?roomTypeId=" + roomTypeId + "&checkIn=" + checkIn + "&checkOut=" + checkOut + "&guests=" + guests;
            }

            Room assignedRoom = availableRooms.get(0);

            // 2. Tìm hoặc lưu thông tin Khách hàng
            Customer customer;
            String idNumber = StringUtils.hasText(identityNumber) ? identityNumber.trim() : null;

            // Ưu tiên tìm theo CCCD/Hộ chiếu (định danh duy nhất) trước
            Optional<Customer> byIdentityOpt = (idNumber != null) ? customerRepository.findByIdentityNumber(idNumber) : Optional.empty();
            // Nếu không có CCCD mới tìm theo số điện thoại
            Optional<Customer> byPhoneOpt = customerRepository.findFirstByPhone(phone.trim());

            if (byIdentityOpt.isPresent()) {
                customer = byIdentityOpt.get();
                customer.setFullName(fullName.trim());
                customer.setPhone(phone.trim());
                if (StringUtils.hasText(email)) customer.setEmail(email.trim());
                customer = customerRepository.save(customer);
            } else if (byPhoneOpt.isPresent()) {
                customer = byPhoneOpt.get();
                customer.setFullName(fullName.trim());
                if (StringUtils.hasText(email)) customer.setEmail(email.trim());
                if (idNumber != null) customer.setIdentityNumber(idNumber);
                customer = customerRepository.save(customer);
            } else {
                customer = Customer.builder()
                        .customerCode(customerRepository.count() > 0 ? "CUST-" + String.format("%06d", customerRepository.count() + 1) : "CUST-000001")
                        .fullName(fullName.trim())
                        .phone(phone.trim())
                        .email(StringUtils.hasText(email) ? email.trim() : null)
                        .identityNumber(idNumber)
                        .active(true)
                        .build();
                customer = customerRepository.save(customer);
            }

            // Gán tài khoản User nếu có
            if (authentication != null && customer.getUser() == null) {
                userRepository.findByUsername(authentication.getName()).ifPresent(customer::setUser);
                customerRepository.save(customer);
            }

            // 3. Tính toán tiền và Voucher
            long nightsLong = ChronoUnit.DAYS.between(checkIn, checkOut);
            int nights = nightsLong > 0 ? (int) nightsLong : 1;
            BigDecimal price = assignedRoom.getRoomType().getPricePerNight();
            BigDecimal roomTotal = price.multiply(BigDecimal.valueOf(nights));

            BigDecimal discount = BigDecimal.ZERO;
            Promotion promotion = null;
            if (StringUtils.hasText(promoCode)) {
                Optional<Promotion> promoOpt = promotionRepository.findByCode(promoCode.trim().toUpperCase());
                if (promoOpt.isPresent() && promoOpt.get().isValidNow()) {
                    promotion = promoOpt.get();
                    discount = promotion.calculateDiscount(roomTotal);
                    promotion.incrementUsage();
                    promotionRepository.save(promotion);
                }
            }
            BigDecimal grandTotal = roomTotal.subtract(discount).max(BigDecimal.ZERO);
            BigDecimal depositAmount = grandTotal.multiply(new BigDecimal("0.3")).setScale(0, java.math.RoundingMode.HALF_UP);

            // 4. Tạo Booking trạng thái PENDING
            Booking booking = Booking.builder()
                    .bookingCode(bookingService.generateBookingCode())
                    .customer(customer)
                    .numberOfGuests(guests)
                    .source(Booking.BookingSource.ONLINE)
                    .checkIn(checkInDt)
                    .checkOut(checkOutDt)
                    .status(Booking.BookingStatus.PENDING)
                    .roomTotalAmount(roomTotal)
                    .serviceTotalAmount(BigDecimal.ZERO)
                    .subtotalAmount(roomTotal)
                    .discountAmount(discount)
                    .totalAmount(grandTotal)
                    .depositAmount(depositAmount)
                    .promotion(promotion)
                    .note(note)
                    .build();

            BookingRoom bookingRoom = BookingRoom.builder()
                    .booking(booking)
                    .room(assignedRoom)
                    .pricePerNight(price)
                    .numberOfNights(nights)
                    .subtotal(roomTotal)
                    .build();

            booking.addBookingRoom(bookingRoom);
            Booking savedBooking = bookingRepository.save(booking);

            // 5. Ghi nhận giao dịch cọc ban đầu
            Payment payment = Payment.builder()
                    .booking(savedBooking)
                    .amount(savedBooking.getDepositAmount())
                    .paymentMethod(paymentMethod)
                    .paymentStatus(Payment.PaymentStatus.PENDING)
                    .transactionCode("TRX-" + System.currentTimeMillis() % 10000000)
                    .note("Đặt cọc 30% trực tuyến đơn " + savedBooking.getBookingCode())
                    .build();
            paymentRepository.save(payment);

            // Mặc định hoặc VietQR / MoMo / Cash -> chuyển về trang kết quả
            return "redirect:/booking/result?code=" + savedBooking.getBookingCode() + "&success=true&method=" + paymentMethod.name();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý đặt phòng: " + e.getMessage());
            return "redirect:/booking/checkout?roomTypeId=" + roomTypeId + "&checkIn=" + checkIn + "&checkOut=" + checkOut;
        }
    }

    /**
     * Màn hình Kết quả Đặt phòng / Thanh toán hợp nhất (payment-result.html)
     */
    @GetMapping({"/booking/result", "/booking/success/{bookingCode}"})
    public String paymentResult(
            @RequestParam(required = false) String code,
            @PathVariable(required = false) String bookingCode,
            @RequestParam(required = false, defaultValue = "true") boolean success,
            @RequestParam(required = false, defaultValue = "false") boolean paid,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String message,
            Model model) {

        String targetCode = StringUtils.hasText(code) ? code : bookingCode;

        if (StringUtils.hasText(targetCode)) {
            Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(targetCode);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();

                // Sinh mã QR VietQR tự động
                String vietQrUrl = vietQrService.generateQrImageUrl(booking.getDepositAmount(), booking.getBookingCode());
                
                // Sinh mã QR MoMo tự động
                String momoQrUrl = momoService.generateMomoQrUrl(booking.getDepositAmount(), booking.getBookingCode());

                model.addAttribute("booking", booking);
                model.addAttribute("success", success);
                model.addAttribute("paid", paid || booking.getStatus() == Booking.BookingStatus.CONFIRMED);
                model.addAttribute("method", method != null ? method : "BANK_TRANSFER");
                model.addAttribute("vietQrUrl", vietQrUrl);
                model.addAttribute("momoQrUrl", momoQrUrl);
                return "client/payment-result";
            }
        }

        // Trường hợp không tìm thấy booking hoặc thất bại
        model.addAttribute("success", false);
        model.addAttribute("paid", false);
        model.addAttribute("message", StringUtils.hasText(message) ? message : "Không tìm thấy thông tin đơn đặt phòng hoặc phiên giao dịch đã hết hạn!");
        return "client/payment-result";
    }
}
