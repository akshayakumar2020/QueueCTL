package com.queuectl.core.model;

public enum JobState {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DEAD;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static JobState fromDb(String value) {
        return JobState.valueOf(value.toUpperCase());
    }
}
