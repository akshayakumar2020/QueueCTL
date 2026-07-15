package com.queuectl.util;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BackoffCalculator {
    public Duration delay(int base, int attempts) {
        if (attempts <= 0) {
            return Duration.ZERO;
        }
        long seconds = 1;
        for (int i = 0; i < attempts; i++) {
            seconds = Math.multiplyExact(seconds, base);
        }
        return Duration.ofSeconds(seconds);
    }
}
