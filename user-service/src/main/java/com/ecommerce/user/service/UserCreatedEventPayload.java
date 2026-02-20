package com.ecommerce.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for UserCreated event stored in outbox.
 * This is the same structure as the shared-lib UserCreatedEvent
 * but kept here to avoid circular dependencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEventPayload {
    private UUID userId;
    private String email;
    private String fullName;
    private long timestamp;
}
