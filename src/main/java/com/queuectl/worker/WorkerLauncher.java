package com.queuectl.worker;

import com.queuectl.core.model.WorkerInfo;
import com.queuectl.core.repository.WorkerRepository;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class WorkerLauncher {
    private final WorkerRepository workerRepository;

    public WorkerLauncher(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    public List<String> start(int count) throws Exception {
        Files.createDirectories(Path.of("data", "workers"));
        Path jar = Path.of("target", "queuectl.jar").toAbsolutePath();
        if (!Files.exists(jar)) {
            jar = Path.of(WorkerLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        }
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = "worker-" + UUID.randomUUID().toString().substring(0, 8);
            ProcessBuilder pb = new ProcessBuilder(javaExecutable(), "-jar", jar.toString(), "internal-worker-run", "--worker-id", id);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File("data/workers/" + id + ".log")));
            pb.redirectError(ProcessBuilder.Redirect.appendTo(new File("data/workers/" + id + ".log")));
            Process process = pb.start();
            workerRepository.register(id, process.pid());
            ids.add(id);
        }
        return ids;
    }

    public int stopAll() {
        workerRepository.removeInactive();
        int signaled = 0;
        for (WorkerInfo worker : workerRepository.listActive()) {
            ProcessHandle.of(worker.pid()).ifPresent(handle -> {
                handle.destroy();
                workerRepository.markStopped(worker.id());
            });
            signaled++;
        }
        return signaled;
    }

    private static String javaExecutable() {
        Path java = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
        return java.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
