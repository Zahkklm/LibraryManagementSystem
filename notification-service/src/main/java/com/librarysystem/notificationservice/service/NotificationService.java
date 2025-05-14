package com.librarysystem.notificationservice.service;

import com.librarysystem.notificationservice.entity.Notification;
import com.librarysystem.notificationservice.event.NotificationEvent;
import com.librarysystem.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    // Sinks for each user (userId -> Sink)
    private final Map<String, Sinks.Many<NotificationEvent>> sinks = new ConcurrentHashMap<>();
    
    /**
     * Save notification and publish to user's stream
     */
    public Mono<Notification> saveAndPublishNotification(NotificationEvent event) {
        // Map to entity
        Notification notification = mapToEntity(event);
        
        // Save to database
        return notificationRepository.save(notification)
            .doOnSuccess(saved -> {
                // Publish to user's stream if they're connected
                publishToUserStream(event);
            });
    }
    
    /**
     * Get notifications for a user
     */
    public Flux<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    /**
     * Get unread notifications for a user
     */
    public Flux<Notification> getUnreadNotificationsForUser(String userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId);
    }
    
    /**
     * Mark notification as read
     */
    public Mono<Notification> markAsRead(String notificationId) {
        return notificationRepository.findById(notificationId)
            .flatMap(notification -> {
                notification.setRead(true);
                return notificationRepository.save(notification);
            });
    }
    
    /**
     * Get notification stream for a user
     */
    public Flux<NotificationEvent> getNotificationStream(String userId) {
        // Create sink if not exists
        Sinks.Many<NotificationEvent> sink = sinks.computeIfAbsent(
            userId,
            key -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        // Create flux from sink
        return sink.asFlux()
            .doOnCancel(() -> {
                log.info("User {} disconnected from notification stream", userId);
            });
    }
    
    /**
     * Publish notification to user's stream
     */
    private void publishToUserStream(NotificationEvent event) {
        Sinks.Many<NotificationEvent> sink = sinks.get(event.getUserId());
        if (sink != null) {
            sink.tryEmitNext(event);
            log.info("Notification sent to user {}: {}", event.getUserId(), event.getMessage());
        }
    }
    
    /**
     * Map DTO to entity
     */
    private Notification mapToEntity(NotificationEvent event) {
        return Notification.builder()
            .id(event.getId())
            .userId(event.getUserId())
            .type(event.getType())
            .message(event.getMessage())
            .details(event.getDetails())
            .timestamp(event.getTimestamp())
            .bookId(event.getBookId())
            .borrowId(event.getBorrowId())
            .build();
    }
    
    /**
     * Map entity to DTO
     */
    private NotificationEvent mapToDto(Notification entity) {
        return NotificationEvent.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .type(entity.getType())
            .message(entity.getMessage())
            .details(entity.getDetails())
            .timestamp(entity.getTimestamp())
            .bookId(entity.getBookId())
            .borrowId(entity.getBorrowId())
            .build();
    }
}
