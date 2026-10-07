package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    Optional<Promotion> findByCode(String code);

    Optional<Promotion> findByCodeAndActiveTrue(String code);

    long countByActiveTrue();
}
