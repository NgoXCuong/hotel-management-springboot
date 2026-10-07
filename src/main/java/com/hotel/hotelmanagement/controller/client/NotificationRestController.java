package com.hotel.hotelmanagement.controller.client;

import com.hotel.hotelmanagement.entity.Notification;
import com.hotel.hotelmanagement.entity.User;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationRestController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<Notification> notifications = notificationService.getNotificationsForUser(user.getId());
        long unreadCount = notificationService.countUnreadForUser(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("notifications", notifications);
        result.put("unreadCount", unreadCount);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok().build();
    }
}