package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository tương tác DB bảng roles.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tìm role theo tên (VD: ROLE_ADMIN, ROLE_CUSTOMER).
     */
    Optional<Role> findByName(String name);

    /**
     * Kiểm tra role đã tồn tại chưa.
     */
    boolean existsByName(String name);
}
