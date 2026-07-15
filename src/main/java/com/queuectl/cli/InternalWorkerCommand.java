package com.queuectl.cli;

import com.queuectl.worker.WorkerRunner;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(name = "internal-worker-run", hidden = true, description = "Internal worker entrypoint.")
public class InternalWorkerCommand implements Callable<Integer> {
    private final WorkerRunner workerRunner;

    @Option(names = "--worker-id", required = true)
    String workerId;

    public InternalWorkerCommand(WorkerRunner workerRunner) {
        this.workerRunner = workerRunner;
    }

    @Override
    public Integer call() {
        return workerRunner.run(workerId);
    }
}
