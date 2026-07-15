package com.queuectl.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuectl.core.model.Job;
import com.queuectl.core.repository.ConfigRepository;
import com.queuectl.core.repository.JobRepository;
import com.queuectl.util.BackoffCalculator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.queuectl.core.model.JobState.PENDING;

@Service
public class QueueService {
    private final JobRepository jobRepository;
    private final ConfigRepository configRepository;
    private final BackoffCalculator backoffCalculator;
    private final ObjectMapper objectMapper;

    public QueueService(JobRepository jobRepository, ConfigRepository configRepository,
                        BackoffCalculator backoffCalculator, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.configRepository = configRepository;
        this.backoffCalculator = backoffCalculator;
        this.objectMapper = objectMapper;
    }

    public Job enqueueJson(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String id = requiredText(node, "id");
        String command = requiredText(node, "command");
        int maxRetries = node.has("max_retries")
                ? node.get("max_retries").asInt()
                : configRepository.getInt("max-retries", 3);
        Instant now = Instant.now();
        Job job = new Job(id, command, PENDING, 0, maxRetries, now, now, now, null, null, null);
        jobRepository.create(job);
        return job;
    }

    public Optional<Job> claim(String workerId) {
        return jobRepository.claimNextPending(workerId, Instant.now());
    }

    public void complete(Job job, String output) {
        jobRepository.markCompleted(job.id(), trim(output), Instant.now());
    }

    public void fail(Job job, String error) {
        int attempts = job.attempts() + 1;
        Instant now = Instant.now();
        if (attempts >= job.maxRetries()) {
            jobRepository.markDead(job.id(), attempts, trim(error), now);
            return;
        }
        int base = configRepository.getInt("backoff-base", 2);
        Instant runAt = now.plus(backoffCalculator.delay(base, attempts));
        jobRepository.scheduleRetry(job.id(), attempts, runAt, trim(error), now);
    }

    private static String requiredText(JsonNode node, String field) {
        if (!node.hasNonNull(field) || node.get(field).asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).asText();
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }
}
