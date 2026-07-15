package com.queuectl.cli;

import com.queuectl.core.model.JobState;
import com.queuectl.core.repository.JobRepository;
import com.queuectl.core.repository.WorkerRepository;
import com.queuectl.util.ConsoleTheme;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Component
@Command(name = "status", mixinStandardHelpOptions = true, description = "Show job state counts and active worker count.")
public class StatusCommand implements Callable<Integer> {
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;

    public StatusCommand(JobRepository jobRepository, WorkerRepository workerRepository) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
    }

    @Override
    public Integer call() {
        workerRepository.removeInactive();
        Map<String, Integer> counts = jobRepository.countsByState();
        List<List<String>> rows = new ArrayList<>();
        for (JobState state : JobState.values()) {
            rows.add(List.of(state.dbValue(), String.valueOf(counts.getOrDefault(state.dbValue(), 0))));
        }
        System.out.println(ConsoleTheme.accent("Queue status"));
        System.out.print(ConsoleTheme.table(List.of("STATE", "COUNT"), rows));
        System.out.println("active workers: " + workerRepository.listActive().size());
        return 0;
    }
}
