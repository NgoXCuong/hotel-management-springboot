package com.hotel.hotelmanagement.dto;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO tổng hợp chỉ số đánh giá toàn khách sạn: Điểm trung bình, tỷ lệ hài lòng và phân bố số sao.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStatsDTO {

    private long totalReviews;
    private double averageRating;
    private int satisfactionRate; // Tỷ lệ đánh giá 4-5 sao (%)
    private long needsAttentionCount; // Số lượng phản hồi <= 3 sao

    private long count5Star;
    private long count4Star;
    private long count3Star;
    private long count2Star;
    private long count1Star;

    public int getPercent5Star() {
        return calculatePercent(count5Star);
    }

    public int getPercent4Star() {
        return calculatePercent(count4Star);
    }

    public int getPercent3Star() {
        return calculatePercent(count3Star);
    }

    public int getPercent2Star() {
        return calculatePercent(count2Star);
    }

    public int getPercent1Star() {
        return calculatePercent(count1Star);
    }

    private int calculatePercent(long count) {
        if (totalReviews == 0) return 0;
        return (int) Math.round((count * 100.0) / totalReviews);
    }
}
