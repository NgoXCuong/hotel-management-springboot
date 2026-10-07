package com.hotel.hotelmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu Webhook từ cổng thanh toán SePay
 * Khi có biến động số dư tiền vào tài khoản ngân hàng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayWebhookDto {

    private Long id;                  // ID giao dịch trên hệ thống SePay
    private String gateway;           // Tên ngân hàng (ví dụ: MBBank)
    private String transactionDate;   // Thời gian xảy ra giao dịch (yyyy-MM-dd HH:mm:ss)
    private String accountNumber;     // Số tài khoản ngân hàng nhận tiền
    private String code;              // Mã thanh toán (nếu SePay nhận diện được)
    private String content;           // Nội dung chuyển khoản (chứa mã booking, ví dụ: BK-20260914-001)
    private String transferType;      // Loại giao dịch: "in" (tiền vào), "out" (tiền ra)
    private BigDecimal transferAmount;// Số tiền chuyển khoản
    private BigDecimal accumulated;   // Số dư tích lũy sau giao dịch
    private String subAccount;        // Tài khoản phụ / VA
    private String referenceCode;     // Mã tham chiếu giao dịch của ngân hàng (FT...)
    private String description;       // Chi tiết toàn bộ nội dung chuyển tiền
}
