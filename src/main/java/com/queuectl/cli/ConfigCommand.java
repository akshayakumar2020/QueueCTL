package com.queuectl.cli;

import com.queuectl.core.repository.ConfigRepository;
import com.queuectl.util.ConsoleTheme;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "config", mixinStandardHelpOptions = true, description = "Read and update queue configuration.",
        subcommands = {ConfigCommand.Get.class, ConfigCommand.Set.class})
public class ConfigCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use config get or config set.");
    }

    @Component
    @Command(name = "get", mixinStandardHelpOptions = true, description = "Get config values.")
    static class Get implements Callable<Integer> {
        private final ConfigRepository configRepository;

        @Parameters(index = "0", arity = "0..1", description = "Config key to read.")
        String key;

        Get(ConfigRepository configRepository) {
            this.configRepository = configRepository;
        }

        @Override
        public Integer call() {
            if (key != null) {
                System.out.println(key + "=" + configRepository.get(key).orElse(""));
            } else {
                List<List<String>> rows = configRepository.list().entrySet().stream()
                        .map(e -> List.of(e.getKey(), e.getValue()))
                        .toList();
                System.out.print(ConsoleTheme.table(List.of("KEY", "VALUE"), rows));
            }
            return 0;
        }
    }

    @Component
    @Command(name = "set", mixinStandardHelpOptions = true, description = "Set a config value.")
    static class Set implements Callable<Integer> {
        private final ConfigRepository configRepository;

        @Parameters(index = "0", description = "Config key, e.g. max-retries or backoff-base.")
        String key;

        @Parameters(index = "1", description = "New value.")
        String value;

        Set(ConfigRepository configRepository) {
            this.configRepository = configRepository;
        }

        @Override
        public Integer call() {
            if (!key.equals("max-retries") && !key.equals("backoff-base")) {
                System.err.println(ConsoleTheme.error("unsupported config key: ") + key);
                return 2;
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed < 1) {
                    throw new NumberFormatException("must be positive");
                }
                configRepository.set(key, value);
                System.out.println(ConsoleTheme.ok("set ") + key + "=" + value);
                return 0;
            } catch (NumberFormatException e) {
                System.err.println(ConsoleTheme.error("value must be a positive integer"));
                return 2;
            }
        }
    }
}
