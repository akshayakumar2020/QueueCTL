package com.queuectl.cli;

import com.queuectl.core.model.Job;
import com.queuectl.core.repository.JobRepository;
import com.queuectl.util.ConsoleTheme;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "dlq", mixinStandardHelpOptions = true, description = "Dead Letter Queue commands.",
        subcommands = {DlqCommand.ListDead.class, DlqCommand.Retry.class})
public class DlqCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use dlq list or dlq retry.");
    }

    @Component
    @Command(name = "list", mixinStandardHelpOptions = true, description = "List dead jobs.")
    static class ListDead implements Callable<Integer> {
        private final JobRepository jobRepository;

        ListDead(JobRepository jobRepository) {
            this.jobRepository = jobRepository;
        }

        @Override
        public Integer call() {
            java.util.List<java.util.List<String>> rows = jobRepository.listDead().stream()
                    .map(this::row)
                    .toList();
            System.out.print(ConsoleTheme.table(java.util.List.of("ID", "ATTEMPTS", "ERROR", "COMMAND"), rows));
            return 0;
        }

        private List<String> row(Job job) {
            return List.of(job.id(), String.valueOf(job.attempts()),
                    job.lastError() == null ? "" : job.lastError(), job.command());
        }
    }

    @Component
    @Command(name = "retry", mixinStandardHelpOptions = true, description = "Requeue a dead job and reset attempts.")
    static class Retry implements Callable<Integer> {
        private final JobRepository jobRepository;

        @Parameters(index = "0", description = "Dead job id to retry.")
        String id;

        Retry(JobRepository jobRepository) {
            this.jobRepository = jobRepository;
        }

        @Override
        public Integer call() {
            boolean updated = jobRepository.retryDead(id, true, Instant.now());
            if (!updated) {
                System.err.println(ConsoleTheme.error("dead job not found: ") + id);
                return 2;
            }
            System.out.println(ConsoleTheme.ok("requeued ") + id);
            return 0;
        }
    }
}
