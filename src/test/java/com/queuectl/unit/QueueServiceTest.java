package com.queuectl.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuectl.TestDatabase;
import com.queuectl.core.model.Job;
import com.queuectl.core.model.JobState;
import com.queuectl.core.service.QueueService;
import com.queuectl.util.BackoffCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueServiceTest {
    @Test
    void malformedJsonIsRejectedCleanly() throws Exception {
        TestDatabase.Fixture db = TestDatabase.create();
        QueueService service = new QueueService(db.jobs(), db.config(), new BackoffCalculator(), new ObjectMapper());

        assertThatThrownBy(() -> service.enqueueJson("{bad-json"))
                .hasMessageContaining("Unexpected character");
    }

    @Test
    void failedJobRetriesThenMovesToDead() throws Exception {
        TestDatabase.Fixture db = TestDatabase.create();
        QueueService service = new QueueService(db.jobs(), db.config(), new BackoffCalculator(), new ObjectMapper());
        Instant now = Instant.now();
        db.jobs().create(new Job("fail", "exit 1", JobState.PROCESSING, 0, 2, now, now, now, "w1", null, null));

        Job first = db.jobs().findById("fail").orElseThrow();
        service.fail(first, "exit 1");
        Job retry = db.jobs().findById("fail").orElseThrow();

        assertThat(retry.state()).isEqualTo(JobState.FAILED);
        assertThat(retry.attempts()).isEqualTo(1);
        assertThat(retry.runAt()).isAfter(retry.updatedAt());

        db.jobs().claimById("fail", "w1", Instant.now());
        service.fail(db.jobs().findById("fail").orElseThrow(), "exit 1 again");

        Job dead = db.jobs().findById("fail").orElseThrow();
        assertThat(dead.state()).isEqualTo(JobState.DEAD);
        assertThat(dead.attempts()).isEqualTo(2);
    }
}
