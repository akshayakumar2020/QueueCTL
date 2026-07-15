package com.queuectl.core.model;

import java.time.Instant;

public record Job(
        String id,
        String command,
        JobState state,
        int attempts,
        int maxRetries,
        Instant createdAt,
        Instant updatedAt,
        Instant runAt,
        String workerId,
        String lastError,
        String output
) {
}
