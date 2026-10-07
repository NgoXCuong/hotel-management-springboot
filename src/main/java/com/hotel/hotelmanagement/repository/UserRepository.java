package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository tương tác DB bảng users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Tìm user theo username (dùng cho đăng nhập).
     */
    Optional<User> findByUsername(String username);

    /**
     * Kiểm tra username đã tồn tại chưa.
     */
    boolean existsByUsername(String username);

    /**
     * Kiểm tra username đã tồn tại chưa (loại trừ user hiện tại khi update).
     */
    boolean existsByUsernameAndIdNot(String username, Long id);

    /**
     * Kiểm tra email đã tồn tại chưa.
     */
    boolean existsByEmail(String email);

    /**
     * Kiểm tra email đã tồn tại chưa (loại trừ user hiện tại khi update).
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Tìm user theo email.
     */
    Optional<User> findByEmail(String email);
}
