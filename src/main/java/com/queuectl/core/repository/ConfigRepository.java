package com.queuectl.core.repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigRepository {
    private final JdbcTemplate jdbcTemplate;

    public ConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> get(String key) {
        return jdbcTemplate.query("SELECT value FROM config WHERE key = ?", (rs, rowNum) -> rs.getString("value"), key)
                .stream()
                .findFirst();
    }

    public int getInt(String key, int fallback) {
        return get(key).map(Integer::parseInt).orElse(fallback);
    }

    public void set(String key, String value) {
        jdbcTemplate.update("""
                INSERT INTO config(key, value, updated_at) VALUES(?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
                """, key, value, Instant.now().toString());
    }

    public Map<String, String> list() {
        return jdbcTemplate.query("SELECT key, value FROM config ORDER BY key", rs -> {
            Map<String, String> values = new LinkedHashMap<>();
            while (rs.next()) {
                values.put(rs.getString("key"), rs.getString("value"));
            }
            return values;
        });
    }
}
