package com.hotel.hotelmanagement.service.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service xử lý tạo mã VietQR chuẩn ngân hàng quốc gia (NAPAS 247).
 * Tích hợp tự động điền Số tiền, Tên tài khoản và Nội dung chuyển khoản theo mã đặt phòng.
 */
@Service
public class VietQrService {

    public static final String BANK_ID = "MB"; // Ngân hàng TMCP Quân Đội (MB Bank)
    public static final String ACCOUNT_NO = "0388545085";
    public static final String ACCOUNT_NAME = "NGO XUAN CUONG";
    public static final String TEMPLATE = "compact2";

    /**
     * Tạo đường dẫn ảnh mã VietQR tự động
     * @param amount Số tiền chuyển khoản
     * @param bookingCode Mã đơn đặt phòng làm nội dung chuyển khoản
     */
    public String generateQrImageUrl(BigDecimal amount, String bookingCode) {
        long amountLong = amount != null ? amount.longValue() : 0L;
        String addInfo = bookingCode != null ? bookingCode.trim() : "GRANDHOTEL";

        try {
            String encodedAccountName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);
            String encodedAddInfo = URLEncoder.encode(addInfo, StandardCharsets.UTF_8);

            return String.format(
                    "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                    BANK_ID, ACCOUNT_NO, TEMPLATE, amountLong, encodedAddInfo, encodedAccountName
            );
        } catch (Exception e) {
            return String.format(
                    "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s",
                    BANK_ID, ACCOUNT_NO, TEMPLATE, amountLong, addInfo
            );
        }
    }
}
