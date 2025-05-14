package com.librarysystem.notificationservice.repository;

import com.librarysystem.notificationservice.entity.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface NotificationRepository extends R2dbcRepository<Notification, String> {
    Flux<Notification> findByUserId(String userId);
    Flux<Notification> findByUserIdAndReadFalse(String userId);
}