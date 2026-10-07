package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    List<Customer> findAll();

    Optional<Customer> findById(Long id);

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByPhone(String phone);

    Customer save(Customer customer);

    void deleteById(Long id);

    void toggleStatus(Long id);

    String generateCustomerCode();

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByIdentityNumber(String identityNumber);

    boolean existsByIdentityNumberAndIdNot(String identityNumber, Long id);

    boolean existsByCustomerCode(String customerCode);

    List<Customer> searchCustomers(String keyword, Customer.Gender gender, String nationality, Boolean active);

    org.springframework.data.domain.Page<Customer> searchCustomers(String keyword, Customer.Gender gender, String nationality, Boolean active, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Customer> findAll(org.springframework.data.domain.Pageable pageable);

    List<String> findDistinctNationalities();

    long countTotal();

    long countActive();

    long countWithAccount();
}
