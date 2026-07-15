package com.queuectl.worker;

import com.queuectl.core.model.Job;
import com.queuectl.core.repository.WorkerRepository;
import com.queuectl.core.service.QueueService;
import com.queuectl.util.ConsoleTheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;

@Component
public class WorkerRunner implements Callable<Integer> {
    private final QueueService queueService;
    private final WorkerRepository workerRepository;
    private final CommandExecutor commandExecutor;
    private final long pollIntervalMs;
    private volatile boolean stopping;
    private String workerId;

    public WorkerRunner(QueueService queueService, WorkerRepository workerRepository,
                        CommandExecutor commandExecutor,
                        @Value("${queuectl.poll-interval-ms:500}") long pollIntervalMs) {
        this.queueService = queueService;
        this.workerRepository = workerRepository;
        this.commandExecutor = commandExecutor;
        this.pollIntervalMs = pollIntervalMs;
    }

    public Integer run(String id) {
        this.workerId = id;
        workerRepository.register(id, ProcessHandle.current().pid());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopping = true));
        System.err.println(ConsoleTheme.muted("worker " + id + " online at " + Instant.now()));
        try {
            while (!stopping) {
                Optional<Job> claimed = queueService.claim(id);
                if (claimed.isEmpty()) {
                    sleep();
                    continue;
                }
                process(claimed.get());
            }
            return 0;
        } finally {
            workerRepository.markStopped(id);
        }
    }

    private void process(Job job) {
        try {
            CommandExecutor.Result result = commandExecutor.execute(job.command());
            if (result.exitCode() == 0) {
                queueService.complete(job, result.output());
            } else {
                queueService.fail(job, "exit " + result.exitCode() + ": " + result.output());
            }
        } catch (Exception e) {
            queueService.fail(job, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void sleep() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException e) {
            stopping = true;
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Integer call() {
        return run(workerId == null ? "worker-" + ProcessHandle.current().pid() : workerId);
    }
}
