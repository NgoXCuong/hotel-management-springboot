package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.dto.ReviewStatsDTO;
import com.hotel.hotelmanagement.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    List<Review> findAll();

    org.springframework.data.domain.Page<Review> findAll(org.springframework.data.domain.Pageable pageable);

    Optional<Review> findById(Long id);

    List<Review> searchReviews(String keyword, Integer rating, Boolean isVisible);

    org.springframework.data.domain.Page<Review> searchReviews(String keyword, Integer rating, Boolean isVisible, org.springframework.data.domain.Pageable pageable);

    ReviewStatsDTO getReviewStats();

    ReviewStatsDTO getPublicReviewStats();

    void toggleVisibility(Long id);

    void delete(Long id);

    Review createReview(Long bookingId, Long customerId, Integer rating, String comment);
}
