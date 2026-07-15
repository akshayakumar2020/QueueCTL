package com.queuectl.cli;

import com.queuectl.util.ConsoleTheme;
import com.queuectl.worker.WorkerLauncher;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "worker", mixinStandardHelpOptions = true, description = "Manage worker JVM processes.",
        subcommands = {WorkerCommand.Start.class, WorkerCommand.Stop.class})
public class WorkerCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use worker start or worker stop.");
    }

    @Component
    @Command(name = "start", mixinStandardHelpOptions = true, description = "Start worker JVM processes.")
    static class Start implements Callable<Integer> {
        private final WorkerLauncher workerLauncher;

        @Option(names = "--count", description = "Number of worker processes to start.", defaultValue = "1")
        int count;

        Start(WorkerLauncher workerLauncher) {
            this.workerLauncher = workerLauncher;
        }

        @Override
        public Integer call() {
            if (count < 1) {
                System.err.println(ConsoleTheme.error("--count must be at least 1"));
                return 2;
            }
            try {
                System.out.print(ConsoleTheme.muted("starting"));
                for (int i = 0; i < Math.min(count, 8); i++) {
                    System.out.print(".");
                    Thread.sleep(80);
                }
                System.out.println();
                List<String> ids = workerLauncher.start(count);
                System.out.println(ConsoleTheme.ok("started workers: ") + String.join(", ", ids));
                return 0;
            } catch (Exception e) {
                System.err.println(ConsoleTheme.error("failed to start workers: ") + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "stop", mixinStandardHelpOptions = true, description = "Gracefully stop running workers.")
    static class Stop implements Callable<Integer> {
        private final WorkerLauncher workerLauncher;

        Stop(WorkerLauncher workerLauncher) {
            this.workerLauncher = workerLauncher;
        }

        @Override
        public Integer call() {
            int signaled = workerLauncher.stopAll();
            System.out.println(ConsoleTheme.ok("signaled workers: ") + signaled);
            return 0;
        }
    }
}
