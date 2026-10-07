package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByBookingId(Long bookingId);

    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByRating(Integer rating);

    long countByIsVisible(Boolean isVisible);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r")
    Double calculateAverageRating();
}
