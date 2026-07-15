package com.queuectl.worker;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class CommandExecutor {
    public Result execute(String command) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> shell = windows
                ? List.of("cmd.exe", "/c", command)
                : List.of("sh", "-c", command);
        Process process = new ProcessBuilder(shell).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> {
            try (var in = process.getInputStream()) {
                in.transferTo(output);
            } catch (Exception ignored) {
            }
        });
        int exit = process.waitFor();
        reader.join(Duration.ofSeconds(2));
        return new Result(exit, output.toString(StandardCharsets.UTF_8));
    }

    public record Result(int exitCode, String output) {
    }
}
