package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByIdentityNumber(String identityNumber);

    boolean existsByIdentityNumberAndIdNot(String identityNumber, Long id);

    boolean existsByCustomerCode(String customerCode);

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findFirstByPhone(String phone);

    Optional<Customer> findByIdentityNumber(String identityNumber);

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findFirstByEmail(String email);

    List<Customer> findByActiveTrue();

    long countByActiveTrue();

    long countByUserIdIsNotNull();
}
