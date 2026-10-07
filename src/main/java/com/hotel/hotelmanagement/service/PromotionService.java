package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Promotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PromotionService {

    List<Promotion> findAll();

    Optional<Promotion> findById(Long id);

    Optional<Promotion> findByCode(String code);

    Promotion save(Promotion promotion);

    void deleteById(Long id);

    void toggleStatus(Long id);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderAmount);

    boolean checkValidity(String code, BigDecimal orderAmount);

    List<Promotion> searchPromotions(String keyword, Promotion.DiscountType discountType, String status);

    org.springframework.data.domain.Page<Promotion> searchPromotions(String keyword, Promotion.DiscountType discountType, String status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Promotion> findAll(org.springframework.data.domain.Pageable pageable);

    long countTotal();

    long countRunning();

    long countExpired();
}
