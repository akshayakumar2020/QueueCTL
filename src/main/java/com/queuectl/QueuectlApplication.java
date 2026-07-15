package com.queuectl;

import com.queuectl.cli.RootCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class QueuectlApplication {
    public static void main(String[] args) {
        System.setProperty("logging.level.root", "ERROR");
        System.setProperty("logging.level.org.springframework", "ERROR");
        System.setProperty("spring.main.log-startup-info", "false");
        System.setProperty("spring.main.banner-mode", "off");
        SpringApplication app = new SpringApplication(QueuectlApplication.class);
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("logging.level.root", "ERROR");
        defaults.put("logging.level.org.springframework", "ERROR");
        defaults.put("spring.main.banner-mode", "off");
        defaults.put("spring.main.log-startup-info", "false");
        app.setDefaultProperties(defaults);
        int exitCode = SpringApplication.exit(app.run(args));
        System.exit(exitCode);
    }

    @Bean
    ExitCodeHolder exitCodeHolder() {
        return new ExitCodeHolder();
    }

    @Bean
    CommandLineRunner runner(RootCommand rootCommand, IFactory factory, ExitCodeHolder exitCodeHolder) {
        return args -> {
            int exitCode = new CommandLine(rootCommand, factory)
                    .setCaseInsensitiveEnumValuesAllowed(true)
                    .setExpandAtFiles(false)
                    .execute(args);
            exitCodeHolder.setExitCode(exitCode);
        };
    }

    static class ExitCodeHolder implements ExitCodeGenerator {
        private int exitCode;

        void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public int getExitCode() {
            return exitCode;
        }
    }
}
