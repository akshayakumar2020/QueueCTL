package com.queuectl;

import com.queuectl.core.repository.ConfigRepository;
import com.queuectl.core.repository.JobRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TestDatabase {
    private TestDatabase() {
    }

    public static Fixture create() throws Exception {
        Path db = Files.createTempFile("queuectl-test-", ".db");
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + db);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("PRAGMA journal_mode=WAL");
        jdbcTemplate.execute("PRAGMA busy_timeout=5000");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        return new Fixture(jdbcTemplate, new JobRepository(jdbcTemplate), new ConfigRepository(jdbcTemplate));
    }

    public record Fixture(JdbcTemplate jdbcTemplate, JobRepository jobs, ConfigRepository config) {
    }
}
