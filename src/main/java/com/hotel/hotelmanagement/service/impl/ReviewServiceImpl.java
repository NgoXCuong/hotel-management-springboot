package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.dto.ReviewStatsDTO;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.entity.Review;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.repository.ReviewRepository;
import com.hotel.hotelmanagement.service.ReviewService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            CustomerRepository customerRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public List<Review> findAll() {
        return reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    @Override
    public org.springframework.data.domain.Page<Review> findAll(org.springframework.data.domain.Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    private Specification<Review> createReviewSpec(String keyword, Integer rating, Boolean isVisible) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                Join<Review, Customer> customerJoin = root.join("customer");
                Join<Review, Booking> bookingJoin = root.join("booking");

                Predicate namePred = cb.like(cb.lower(customerJoin.get("fullName")), term);
                Predicate phonePred = cb.like(cb.lower(customerJoin.get("phone")), term);
                Predicate bookingCodePred = cb.like(cb.lower(bookingJoin.get("bookingCode")), term);
                Predicate commentPred = cb.like(cb.lower(root.get("comment")), term);

                predicates.add(cb.or(namePred, phonePred, bookingCodePred, commentPred));
            }

            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            if (isVisible != null) {
                predicates.add(cb.equal(root.get("isVisible"), isVisible));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<Review> searchReviews(String keyword, Integer rating, Boolean isVisible) {
        return reviewRepository.findAll(createReviewSpec(keyword, rating, isVisible), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public org.springframework.data.domain.Page<Review> searchReviews(String keyword, Integer rating, Boolean isVisible, org.springframework.data.domain.Pageable pageable) {
        return reviewRepository.findAll(createReviewSpec(keyword, rating, isVisible), pageable);
    }

    @Override
    public ReviewStatsDTO getReviewStats() {
        return computeStats(reviewRepository.findAll());
    }

    @Override
    public ReviewStatsDTO getPublicReviewStats() {
        return computeStats(searchReviews(null, null, true));
    }

    private ReviewStatsDTO computeStats(List<Review> all) {
        long total = all.size();

        if (total == 0) {
            return ReviewStatsDTO.builder()
                    .totalReviews(0)
                    .averageRating(5.0)
                    .satisfactionRate(100)
                    .needsAttentionCount(0)
                    .count5Star(0)
                    .count4Star(0)
                    .count3Star(0)
                    .count2Star(0)
                    .count1Star(0)
                    .build();
        }

        long count5 = all.stream().filter(r -> r.getRating() != null && r.getRating() == 5).count();
        long count4 = all.stream().filter(r -> r.getRating() != null && r.getRating() == 4).count();
        long count3 = all.stream().filter(r -> r.getRating() != null && r.getRating() == 3).count();
        long count2 = all.stream().filter(r -> r.getRating() != null && r.getRating() == 2).count();
        long count1 = all.stream().filter(r -> r.getRating() != null && r.getRating() == 1).count();

        double avg = all.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .orElse(5.0);

        double roundedAvg = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();

        long positiveCount = count5 + count4;
        int satisfactionRate = (int) Math.round((positiveCount * 100.0) / total);
        long needsAttention = count3 + count2 + count1;

        return ReviewStatsDTO.builder()
                .totalReviews(total)
                .averageRating(roundedAvg)
                .satisfactionRate(satisfactionRate)
                .needsAttentionCount(needsAttention)
                .count5Star(count5)
                .count4Star(count4)
                .count3Star(count3)
                .count2Star(count2)
                .count1Star(count1)
                .build();
    }

    @Override
    @Transactional
    public void toggleVisibility(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá ID: " + id));
        review.setIsVisible(!Boolean.TRUE.equals(review.getIsVisible()));
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy đánh giá ID: " + id);
        }
        reviewRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Review createReview(Long bookingId, Long customerId, Integer rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng ID: " + bookingId));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng ID: " + customerId));

        Review review = Review.builder()
                .booking(booking)
                .customer(customer)
                .rating(rating != null ? Math.min(5, Math.max(1, rating)) : 5)
                .comment(comment)
                .isVisible(true)
                .build();

        return reviewRepository.save(review);
    }
}
