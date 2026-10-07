package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();

    org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    List<User> searchUsers(String keyword, Long roleId, Boolean enabled, Boolean accountNonLocked);

    org.springframework.data.domain.Page<User> searchUsers(String keyword, Long roleId, Boolean enabled, Boolean accountNonLocked, org.springframework.data.domain.Pageable pageable);

    User createUser(User user, List<Long> roleIds, String rawPassword);

    User updateUser(Long id, User userDetails, List<Long> roleIds, String newRawPassword);

    void toggleAccountLock(Long id);

    void toggleEnabled(Long id);

    void resetPassword(Long id, String newPassword);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countTotal();

    long countActive();

    long countLocked();
}
