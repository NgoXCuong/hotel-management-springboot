package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.Notification;
import com.hotel.hotelmanagement.repository.NotificationRepository;
import com.hotel.hotelmanagement.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countUnreadForUser(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public Notification markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        n.setRead(true);
        return notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Override
    @Transactional
    public Notification create(Long userId, String title, String message, String icon, String link) {
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .icon(icon != null ? icon : "fa-solid fa-bell")
                .link(link)
                .read(false)
                .build();
        return notificationRepository.save(n);
    }
}