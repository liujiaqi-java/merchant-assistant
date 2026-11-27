package com.merchantassistant.notification;

import java.time.Instant;

public record NotificationView(
        Long id,
        NotificationType type,
        String title,
        String message,
        String targetView,
        String referenceType,
        Long referenceId,
        boolean read,
        Instant createdAt,
        Instant readAt
) { }
