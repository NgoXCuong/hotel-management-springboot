package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<Payment> findByTransactionCode(String transactionCode);
}
