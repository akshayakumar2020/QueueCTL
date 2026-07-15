package com.queuectl.cli;

import com.queuectl.core.model.Job;
import com.queuectl.core.model.JobState;
import com.queuectl.core.repository.JobRepository;
import com.queuectl.util.ConsoleTheme;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Component
@Command(name = "list", mixinStandardHelpOptions = true, description = "List jobs, optionally filtered by state.")
public class ListCommand implements Callable<Integer> {
    private final JobRepository jobRepository;

    @Option(names = "--state", description = "Filter by state: pending, processing, completed, failed, dead")
    JobState state;

    public ListCommand(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public Integer call() {
        List<List<String>> rows = jobRepository.list(Optional.ofNullable(state)).stream()
                .map(this::row)
                .toList();
        System.out.print(ConsoleTheme.table(List.of("ID", "STATE", "ATTEMPTS", "RUN AT", "COMMAND"), rows));
        return 0;
    }

    private List<String> row(Job job) {
        return List.of(job.id(), job.state().dbValue(), String.valueOf(job.attempts()),
                job.runAt().toString(), job.command());
    }
}
