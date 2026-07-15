package com.queuectl.core.model;

import java.time.Instant;

public record WorkerInfo(String id, long pid, String state, Instant startedAt, Instant updatedAt) {
}
