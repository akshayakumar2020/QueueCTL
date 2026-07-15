package com.queuectl.unit;

import com.queuectl.TestDatabase;
import com.queuectl.core.model.Job;
import com.queuectl.core.model.JobState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class JobRepositoryTest {
    @Test
    void insertsAndReadsBackJobFields() throws Exception {
        TestDatabase.Fixture db = TestDatabase.create();
        Instant now = Instant.parse("2026-07-15T10:00:00Z");
        Job job = new Job("job1", "echo hello", JobState.PENDING, 0, 3, now, now, now, null, null, null);

        db.jobs().create(job);

        assertThat(db.jobs().findById("job1")).contains(job);
    }

    @Test
    void concurrentClaimOnlyAllowsOneWinner() throws Exception {
        TestDatabase.Fixture db = TestDatabase.create();
        Instant now = Instant.now();
        db.jobs().create(new Job("job-race", "echo race", JobState.PENDING, 0, 3, now, now, now, null, null, null));

        Callable<Optional<Job>> claim = () -> db.jobs().claimNextPending(Thread.currentThread().getName(), Instant.now());
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Optional<Job>> results = new ArrayList<>();
            for (var future : pool.invokeAll(List.of(claim, claim))) {
                results.add(future.get());
            }

            assertThat(results.stream().filter(Optional::isPresent)).hasSize(1);
            assertThat(db.jobs().findById("job-race").orElseThrow().state()).isEqualTo(JobState.PROCESSING);
        }
    }
}
