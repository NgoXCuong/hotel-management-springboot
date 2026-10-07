package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.service.CustomerService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Customer> findByCustomerCode(String customerCode) {
        return customerRepository.findByCustomerCode(customerCode);
    }

    @Override
    public Optional<Customer> findByPhone(String phone) {
        if (!StringUtils.hasText(phone)) return Optional.empty();
        return customerRepository.findFirstByPhone(phone.trim());
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        // Tự động sinh mã khách hàng nếu chưa có
        if (!StringUtils.hasText(customer.getCustomerCode())) {
            customer.setCustomerCode(generateCustomerCode());
        }

        // Chuẩn hóa dữ liệu văn bản
        if (customer.getFullName() != null) {
            customer.setFullName(customer.getFullName().trim());
        }
        if (customer.getPhone() != null) {
            customer.setPhone(customer.getPhone().trim());
        }
        if (customer.getEmail() != null) {
            customer.setEmail(customer.getEmail().trim().toLowerCase());
            if (customer.getEmail().isBlank()) {
                customer.setEmail(null);
            }
        }
        if (customer.getIdentityNumber() != null) {
            customer.setIdentityNumber(customer.getIdentityNumber().trim());
            if (customer.getIdentityNumber().isBlank()) {
                customer.setIdentityNumber(null);
            }
        }
        if (customer.getAddress() != null) {
            customer.setAddress(customer.getAddress().trim());
            if (customer.getAddress().isBlank()) {
                customer.setAddress(null);
            }
        }
        if (!StringUtils.hasText(customer.getNationality())) {
            customer.setNationality("Việt Nam");
        } else {
            customer.setNationality(customer.getNationality().trim());
        }

        if (customer.getActive() == null) {
            customer.setActive(true);
        }

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        customerRepository.findById(id).ifPresent(customer -> {
            customer.setActive(!customer.getActive());
            customerRepository.save(customer);
        });
    }

    @Override
    public String generateCustomerCode() {
        String code;
        do {
            // Định dạng CUST-XXXXXX (6 chữ số đệm 0)
            int randomNum = secureRandom.nextInt(1_000_000);
            code = String.format("CUST-%06d", randomNum);
        } while (customerRepository.existsByCustomerCode(code));
        return code;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) return false;
        return customerRepository.existsByPhone(phone.trim());
    }

    @Override
    public boolean existsByPhoneAndIdNot(String phone, Long id) {
        if (!StringUtils.hasText(phone) || id == null) return false;
        return customerRepository.existsByPhoneAndIdNot(phone.trim(), id);
    }

    @Override
    public boolean existsByIdentityNumber(String identityNumber) {
        if (!StringUtils.hasText(identityNumber)) return false;
        return customerRepository.existsByIdentityNumber(identityNumber.trim());
    }

    @Override
    public boolean existsByIdentityNumberAndIdNot(String identityNumber, Long id) {
        if (!StringUtils.hasText(identityNumber) || id == null) return false;
        return customerRepository.existsByIdentityNumberAndIdNot(identityNumber.trim(), id);
    }

    @Override
    public boolean existsByCustomerCode(String customerCode) {
        if (!StringUtils.hasText(customerCode)) return false;
        return customerRepository.existsByCustomerCode(customerCode.trim());
    }

    private Specification<Customer> createCustomerSpec(String keyword, Customer.Gender gender, String nationality, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm kiếm đa năng (Tên, SĐT, Email, CCCD, Mã khách)
            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchPattern);
                Predicate idMatch = cb.like(cb.lower(root.get("identityNumber")), searchPattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("customerCode")), searchPattern);

                predicates.add(cb.or(nameMatch, phoneMatch, emailMatch, idMatch, codeMatch));
            }

            // 2. Lọc theo Giới tính
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            // 3. Lọc theo Quốc tịch
            if (StringUtils.hasText(nationality)) {
                predicates.add(cb.equal(cb.lower(root.get("nationality")), nationality.trim().toLowerCase()));
            }

            // 4. Lọc theo Trạng thái hoạt động
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<Customer> searchCustomers(String keyword, Customer.Gender gender, String nationality, Boolean active) {
        return customerRepository.findAll(createCustomerSpec(keyword, gender, nationality, active), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public org.springframework.data.domain.Page<Customer> searchCustomers(String keyword, Customer.Gender gender, String nationality, Boolean active, org.springframework.data.domain.Pageable pageable) {
        return customerRepository.findAll(createCustomerSpec(keyword, gender, nationality, active), pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Customer> findAll(org.springframework.data.domain.Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Override
    public List<String> findDistinctNationalities() {
        return customerRepository.findAll().stream()
                .map(Customer::getNationality)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public long countTotal() {
        return customerRepository.count();
    }

    @Override
    public long countActive() {
        return customerRepository.countByActiveTrue();
    }

    @Override
    public long countWithAccount() {
        return customerRepository.countByUserIdIsNotNull();
    }
}
