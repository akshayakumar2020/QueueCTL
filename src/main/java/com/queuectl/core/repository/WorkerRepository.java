package com.queuectl.core.repository;

import com.queuectl.core.model.WorkerInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class WorkerRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<WorkerInfo> mapper = new WorkerRowMapper();

    public WorkerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void register(String id, long pid) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO workers(id, pid, state, started_at, updated_at) VALUES(?, ?, 'running', ?, ?)
                ON CONFLICT(id) DO UPDATE SET pid = excluded.pid, state = 'running', updated_at = excluded.updated_at
                """, id, pid, now.toString(), now.toString());
    }

    public void markStopped(String id) {
        jdbcTemplate.update("UPDATE workers SET state = 'stopped', updated_at = ? WHERE id = ?", Instant.now().toString(), id);
    }

    public List<WorkerInfo> listActive() {
        return jdbcTemplate.query("SELECT * FROM workers WHERE state = 'running' ORDER BY started_at", mapper);
    }

    public void removeInactive() {
        for (WorkerInfo worker : listActive()) {
            if (ProcessHandle.of(worker.pid()).isEmpty() || !ProcessHandle.of(worker.pid()).get().isAlive()) {
                markStopped(worker.id());
            }
        }
    }

    private static class WorkerRowMapper implements RowMapper<WorkerInfo> {
        @Override
        public WorkerInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new WorkerInfo(
                    rs.getString("id"),
                    rs.getLong("pid"),
                    rs.getString("state"),
                    Instant.parse(rs.getString("started_at")),
                    Instant.parse(rs.getString("updated_at"))
            );
        }
    }
}
