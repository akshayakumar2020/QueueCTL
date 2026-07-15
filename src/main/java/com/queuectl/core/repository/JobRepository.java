package com.queuectl.core.repository;

import com.queuectl.core.model.Job;
import com.queuectl.core.model.JobState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JobRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Job> mapper = new JobRowMapper();

    public JobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Job job) {
        jdbcTemplate.update("""
                INSERT INTO jobs(id, command, state, attempts, max_retries, created_at, updated_at, run_at, worker_id, last_error, output)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, job.id(), job.command(), job.state().dbValue(), job.attempts(), job.maxRetries(),
                job.createdAt().toString(), job.updatedAt().toString(), job.runAt().toString(),
                job.workerId(), job.lastError(), job.output());
    }

    public Optional<Job> findById(String id) {
        List<Job> jobs = jdbcTemplate.query("SELECT * FROM jobs WHERE id = ?", mapper, id);
        return jobs.stream().findFirst();
    }

    public List<Job> list(Optional<JobState> state) {
        if (state.isPresent()) {
            return jdbcTemplate.query("SELECT * FROM jobs WHERE state = ? ORDER BY created_at", mapper, state.get().dbValue());
        }
        return jdbcTemplate.query("SELECT * FROM jobs ORDER BY created_at", mapper);
    }

    public List<Job> listDead() {
        return list(Optional.of(JobState.DEAD));
    }

    public Map<String, Integer> countsByState() {
        return jdbcTemplate.query("SELECT state, COUNT(*) count FROM jobs GROUP BY state", rs -> {
            java.util.LinkedHashMap<String, Integer> counts = new java.util.LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("state"), rs.getInt("count"));
            }
            return counts;
        });
    }

    @Transactional
    public Optional<Job> claimNextPending(String workerId, Instant now) {
        List<String> candidates = jdbcTemplate.query("""
                SELECT id FROM jobs
                WHERE state IN ('pending', 'failed') AND run_at <= ?
                ORDER BY run_at, created_at
                LIMIT 1
                """, (rs, rowNum) -> rs.getString("id"), now.toString());
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        String id = candidates.getFirst();
        int updated = jdbcTemplate.update("""
                UPDATE jobs
                SET state = 'processing', worker_id = ?, updated_at = ?
                WHERE id = ? AND state IN ('pending', 'failed')
                """, workerId, now.toString(), id);
        if (updated != 1) {
            return Optional.empty();
        }
        return findById(id);
    }

    public boolean claimById(String id, String workerId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE jobs SET state = 'processing', worker_id = ?, updated_at = ?
                WHERE id = ? AND state IN ('pending', 'failed')
                """, workerId, now.toString(), id) == 1;
    }

    public void markCompleted(String id, String output, Instant now) {
        jdbcTemplate.update("""
                UPDATE jobs SET state = 'completed', output = ?, last_error = NULL, worker_id = NULL, updated_at = ?
                WHERE id = ?
                """, output, now.toString(), id);
    }

    public void scheduleRetry(String id, int attempts, Instant runAt, String error, Instant now) {
        jdbcTemplate.update("""
                UPDATE jobs
                SET state = 'failed', attempts = ?, run_at = ?, last_error = ?, worker_id = NULL, updated_at = ?
                WHERE id = ?
                """, attempts, runAt.toString(), error, now.toString(), id);
    }

    public void markDead(String id, int attempts, String error, Instant now) {
        jdbcTemplate.update("""
                UPDATE jobs
                SET state = 'dead', attempts = ?, last_error = ?, worker_id = NULL, updated_at = ?
                WHERE id = ?
                """, attempts, error, now.toString(), id);
    }

    public boolean retryDead(String id, boolean resetAttempts, Instant now) {
        int attempts = resetAttempts ? 0 : findById(id).map(Job::attempts).orElse(0);
        return jdbcTemplate.update("""
                UPDATE jobs
                SET state = 'pending', attempts = ?, run_at = ?, worker_id = NULL, last_error = NULL, updated_at = ?
                WHERE id = ? AND state = 'dead'
                """, attempts, now.toString(), now.toString(), id) == 1;
    }

    public int resetStaleProcessing(Instant before) {
        return jdbcTemplate.update("""
                UPDATE jobs SET state = 'pending', worker_id = NULL, updated_at = ?
                WHERE state = 'processing' AND updated_at < ?
                """, Instant.now().toString(), before.toString());
    }

    private static class JobRowMapper implements RowMapper<Job> {
        @Override
        public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Job(
                    rs.getString("id"),
                    rs.getString("command"),
                    JobState.fromDb(rs.getString("state")),
                    rs.getInt("attempts"),
                    rs.getInt("max_retries"),
                    Instant.parse(rs.getString("created_at")),
                    Instant.parse(rs.getString("updated_at")),
                    Instant.parse(rs.getString("run_at")),
                    rs.getString("worker_id"),
                    rs.getString("last_error"),
                    rs.getString("output")
            );
        }
    }
}
