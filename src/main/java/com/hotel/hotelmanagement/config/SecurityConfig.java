package com.hotel.hotelmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isStaffOrAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_ADMIN") ||
                            grantedAuthority.getAuthority().equals("ROLE_MANAGER") ||
                            grantedAuthority.getAuthority().equals("ROLE_RECEPTIONIST"));

            if (isStaffOrAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/sepay/**", "/api/booking/**"))
            // ============ PHÂN QUYỀN URL ============
            .authorizeHttpRequests(auth -> auth
                .requestMatchers( "/", "/rooms/**", "/booking/**", "/api/sepay/**",
                    "/api/booking/**", "/services/**", "/about/**", "/contact/**",
                    "/reviews/**", "/auth/**", "/error", "/error/**", "/css/**",
                    "/js/**", "/images/**", "/webjars/**").permitAll()

                // Khách hàng đăng nhập xem tài khoản và lịch sử chuyến đi
                .requestMatchers("/my-account/**", "/my-bookings/**").authenticated()

                // Quản trị viên / Quản lý / Lễ tân truy cập /admin/**
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                // Mọi request khác
                .anyRequest().authenticated()
            )

            // ============ FORM LOGIN ============
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(customAuthenticationSuccessHandler())
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )

            // ============ LOGOUT ============
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .exceptionHandling(ex -> ex
                .accessDeniedPage("/auth/403")
            );
        return http.build();
    }
}
