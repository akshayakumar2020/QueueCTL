package com.queuectl.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(
        name = "queuectl",
        mixinStandardHelpOptions = true,
        version = "queuectl 0.1.0",
        description = "Persistent background job queue CLI",
        subcommands = {
                EnqueueCommand.class,
                ListCommand.class,
                StatusCommand.class,
                WorkerCommand.class,
                DlqCommand.class,
                ConfigCommand.class,
                InternalWorkerCommand.class
        }
)
public class RootCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use --help to see available commands.");
    }
}
