package com.queuectl.cli;

import java.io.File;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

@Component
@Command(name = "demo", description = "Initialize demo environment and print instructions.")
public class DemoCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        // Initialize environment by deleting data dir if it exists
        File dataDir = new File("data");
        if (dataDir.exists()) {
            deleteDirectory(dataDir);
        }
        dataDir.mkdirs();

        System.out.println("=========================================");
        System.out.println("QueueCTL Demo");
        System.out.println("=========================================");
        System.out.println();
        System.out.println("Project initialized successfully.");
        System.out.println();
        System.out.println("Now use QueueCTL manually.");
        System.out.println();
        System.out.println("Example commands:");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 enqueue '{\"id\":\"job1\",\"command\":\"echo hello\"}'");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 enqueue '{\"id\":\"job2\",\"command\":\"cmd /c exit 1\"}'");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 list");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 config set backoff-base 1");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 worker start --count 2");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 list");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 status");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 dlq list");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 dlq retry job2");
        System.out.println();
        System.out.println(".\\bin\\queuectl.ps1 worker stop");

        return 0;
    }

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
