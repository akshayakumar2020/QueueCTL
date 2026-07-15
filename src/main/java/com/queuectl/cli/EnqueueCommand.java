package com.queuectl.cli;

import com.queuectl.core.model.Job;
import com.queuectl.core.service.QueueService;
import com.queuectl.util.ConsoleTheme;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Component
@Command(name = "enqueue", mixinStandardHelpOptions = true, description = "Add a JSON job to the queue.")
public class EnqueueCommand implements Callable<Integer> {
    private final QueueService queueService;

    @Parameters(index = "0..*", arity = "1..*", description = "JSON job payload or @file, e.g. '{\"id\":\"job1\",\"command\":\"echo hello\"}'")
    String[] payload;

    public EnqueueCommand(QueueService queueService) {
        this.queueService = queueService;
    }

    @Override
    public Integer call() {
        try {
            Job job = queueService.enqueueJson(readPayload());
            System.out.println(ConsoleTheme.ok("enqueued ") + job.id());
            return 0;
        } catch (DuplicateKeyException e) {
            System.err.println(ConsoleTheme.error("job id already exists"));
            return 2;
        } catch (Exception e) {
            System.err.println(ConsoleTheme.error("invalid job: ") + e.getMessage());
            return 2;
        }
    }

    private String readPayload() throws Exception {
        String joined = String.join(" ", payload);
        if (joined.startsWith("@")) {
            return Files.readString(Path.of(joined.substring(1)));
        }
        return joined;
    }
}
