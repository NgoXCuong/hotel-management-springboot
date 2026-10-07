package com.hotel.hotelmanagement.service.payment;

import com.hotel.hotelmanagement.entity.Booking;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service xử lý thanh toán Ví điện tử MoMo.
 * Hỗ trợ tạo mã MoMo QR thanh toán nhanh & liên kết điều hướng ứng dụng MoMo.
 */
@Service
public class MomoService {

    public static final String MOMO_PHONE = "0912345678";
    public static final String MOMO_RECEIVER_NAME = "GRAND HOTEL RESORT";

    /**
     * Tạo đường dẫn mã MoMo QR thanh toán
     */
    public String generateMomoQrUrl(BigDecimal amount, String bookingCode) {
        long amountLong = amount != null ? amount.longValue() : 0L;
        String note = "GRANDHOTEL " + (bookingCode != null ? bookingCode : "");

        try {
            // Định dạng payload chuẩn MoMo P2P QR
            String rawData = String.format(
                    "2|99|%s|%s|contact@grandhotel.vn|0|0|%d|%s|transfer_myqr",
                    MOMO_PHONE, MOMO_RECEIVER_NAME, amountLong, note
            );
            String encodedData = URLEncoder.encode(rawData, StandardCharsets.UTF_8);
            return "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + encodedData;
        } catch (Exception e) {
            return "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=MOMO_" + bookingCode;
        }
    }
}
