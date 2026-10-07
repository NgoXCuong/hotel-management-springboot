package com.hotel.hotelmanagement.config;

import com.hotel.hotelmanagement.entity.Role;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.RoleRepository;
import com.hotel.hotelmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Tự động nạp tài khoản quản trị ADMIN vào cơ sở dữ liệu khi khởi động ứng dụng (nếu chưa có).
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initAdminAccount(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // 1. Đảm bảo role ROLE_ADMIN tồn tại
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        log.info("Tạo role ROLE_ADMIN...");
                        return roleRepository.save(
                                Role.builder()
                                        .name("ROLE_ADMIN")
                                        .description("Quản trị hệ thống - toàn quyền cấu hình và quản trị")
                                        .build()
                        );
                    });

            // Khởi tạo các vai trò cơ bản của hệ thống nếu chưa có
            if (!roleRepository.existsByName("ROLE_MANAGER")) {
                roleRepository.save(Role.builder().name("ROLE_MANAGER").description("Quản lý khách sạn").build());
            }
            if (!roleRepository.existsByName("ROLE_RECEPTIONIST")) {
                roleRepository.save(Role.builder().name("ROLE_RECEPTIONIST").description("Lễ tân").build());
            }
            if (!roleRepository.existsByName("ROLE_CUSTOMER")) {
                roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng").build());
            }

            // 2. Tự động tạo tài khoản ADMIN mặc định (chỉ tài khoản admin)
            if (!userRepository.existsByUsername("admin")) {
                log.info("Đang tự động nạp tài khoản ADMIN vào database (admin / admin123)...");

                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("System Administrator")
                        .email("admin@hotel.com")
                        .phone("0900000001")
                        .enabled(true)
                        .accountNonLocked(true)
                        .failedLoginAttempts(0)
                        .roles(Set.of(adminRole))
                        .build();

                userRepository.save(admin);
                log.info("✅ Tài khoản ADMIN (admin / admin123) đã được tự động nạp vào DB thành công!");
            }
        };
    }
}

