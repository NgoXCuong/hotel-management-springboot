package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Notification;

import java.util.List;

public interface NotificationService {

    List<Notification> getNotificationsForUser(Long userId);

    long countUnreadForUser(Long userId);

    Notification markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);

    Notification create(Long userId, String title, String message, String icon, String link);
}
