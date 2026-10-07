package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.Role;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.RoleRepository;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    private Specification<User> createUserSpec(String keyword, Long roleId, Boolean enabled, Boolean accountNonLocked) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                Predicate usernamePred = cb.like(cb.lower(root.get("username")), term);
                Predicate fullNamePred = cb.like(cb.lower(root.get("fullName")), term);
                Predicate emailPred = cb.like(cb.lower(root.get("email")), term);
                Predicate phonePred = cb.like(cb.lower(root.get("phone")), term);

                predicates.add(cb.or(usernamePred, fullNamePred, emailPred, phonePred));
            }

            if (roleId != null) {
                Join<User, Role> roleJoin = root.join("roles");
                predicates.add(cb.equal(roleJoin.get("id"), roleId));
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            if (accountNonLocked != null) {
                predicates.add(cb.equal(root.get("accountNonLocked"), accountNonLocked));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<User> searchUsers(String keyword, Long roleId, Boolean enabled, Boolean accountNonLocked) {
        return userRepository.findAll(createUserSpec(keyword, roleId, enabled, accountNonLocked), Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public org.springframework.data.domain.Page<User> searchUsers(String keyword, Long roleId, Boolean enabled, Boolean accountNonLocked, org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(createUserSpec(keyword, roleId, enabled, accountNonLocked), pageable);
    }

    @Override
    @Transactional
    public User createUser(User user, List<Long> roleIds, String rawPassword) {
        if (existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + user.getUsername() + "' đã tồn tại!");
        }

        if (StringUtils.hasText(user.getEmail()) && existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' đã được sử dụng!");
        }

        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Mật khẩu không được để trống khi tạo tài khoản mới!");
        }

        user.setPassword(passwordEncoder.encode(rawPassword));

        if (roleIds != null && !roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findAllById(roleIds);
            user.setRoles(new HashSet<>(roles));
        } else {
            roleRepository.findByName("ROLE_RECEPTIONIST").ifPresent(r -> user.setRoles(Set.of(r)));
        }

        if (user.getEnabled() == null) user.setEnabled(true);
        if (user.getAccountNonLocked() == null) user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userDetails, List<Long> roleIds, String newRawPassword) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + id));

        if (existsByUsernameAndIdNot(userDetails.getUsername(), id)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + userDetails.getUsername() + "' đã được sử dụng bởi tài khoản khác!");
        }

        if (StringUtils.hasText(userDetails.getEmail()) && existsByEmailAndIdNot(userDetails.getEmail(), id)) {
            throw new IllegalArgumentException("Email '" + userDetails.getEmail() + "' đã được sử dụng bởi tài khoản khác!");
        }

        existingUser.setUsername(userDetails.getUsername().trim());
        existingUser.setFullName(userDetails.getFullName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setAvatarUrl(userDetails.getAvatarUrl());

        if (userDetails.getEnabled() != null) {
            existingUser.setEnabled(userDetails.getEnabled());
        }
        if (userDetails.getAccountNonLocked() != null) {
            existingUser.setAccountNonLocked(userDetails.getAccountNonLocked());
            if (Boolean.TRUE.equals(userDetails.getAccountNonLocked())) {
                existingUser.setFailedLoginAttempts(0);
            }
        }

        if (StringUtils.hasText(newRawPassword)) {
            existingUser.setPassword(passwordEncoder.encode(newRawPassword));
        }

        if (roleIds != null) {
            List<Role> roles = roleRepository.findAllById(roleIds);
            existingUser.setRoles(new HashSet<>(roles));
        }

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void toggleAccountLock(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + id));

        boolean isCurrentlyLocked = !Boolean.TRUE.equals(user.getAccountNonLocked());
        if (isCurrentlyLocked) {
            // Mở khóa tài khoản: Reset failed attempts và chuyển sang unlocked
            user.setAccountNonLocked(true);
            user.setFailedLoginAttempts(0);
        } else {
            // Khóa tài khoản
            user.setAccountNonLocked(false);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + id));
        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống!");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByUsernameAndIdNot(String username, Long id) {
        return userRepository.existsByUsernameAndIdNot(username, id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return userRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public long countTotal() {
        return userRepository.count();
    }

    @Override
    public long countActive() {
        return userRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getEnabled()) && Boolean.TRUE.equals(u.getAccountNonLocked())).count();
    }

    @Override
    public long countLocked() {
        return userRepository.findAll().stream().filter(u -> Boolean.FALSE.equals(u.getAccountNonLocked())).count();
    }
}
