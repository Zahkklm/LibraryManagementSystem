package com.librarysystem.notificationservice.controller;

import com.librarysystem.notificationservice.entity.Notification;
import com.librarysystem.notificationservice.event.NotificationEvent;
import com.librarysystem.notificationservice.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller for notification-related operations.
 * <p>
 * This controller provides endpoints for managing user notifications including:
 * <ul>
 *   <li>Real-time notification streaming via Server-Sent Events (SSE)</li>
 *   <li>Retrieving all notifications for a user</li>
 * </ul>
 * <p>
 * All endpoints are reactive, returning Flux types for streaming data.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for managing user notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * Streams real-time notifications for a specific user.
     * <p>
     * This endpoint uses Server-Sent Events (SSE) to establish a persistent connection
     * with the client. New notifications for the specified user will be pushed to the
     * client as they occur.
     * 
     * @param userId The unique identifier of the user
     * @return A Flux of NotificationEvent objects as a stream
     */
    @Operation(
        summary = "Stream real-time notifications",
        description = "Establishes an SSE connection to receive real-time notifications for a specific user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful connection established",
            content = @Content(
                mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = NotificationEvent.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NotificationEvent> streamNotifications(
            @Parameter(description = "User ID to retrieve notifications for", required = true)
            @PathVariable String userId) {
        return notificationService.getNotificationStream(userId);
    }
    
    /**
     * Retrieves all notifications for a specific user.
     * <p>
     * This endpoint returns all notifications associated with the specified user ID.
     * 
     * @param userId The unique identifier of the user
     * @return A Flux of Notification objects
     */
    @Operation(
        summary = "Get all user notifications",
        description = "Retrieves all notifications for a specific user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notifications successfully retrieved",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = Notification.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{userId}")
    public Flux<Notification> getNotifications(
            @Parameter(description = "User ID to retrieve notifications for", required = true)
            @PathVariable String userId) {
        return notificationService.getNotificationsForUser(userId);
    }
}
