package com.queuectl.unit;

import com.queuectl.worker.CommandExecutor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandExecutorTest {
    @Test
    void successfulCommandReturnsOutput() throws Exception {
        CommandExecutor.Result result = new CommandExecutor().execute("echo hello");

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains("hello");
    }

    @Test
    void failingCommandReturnsNonZeroExit() throws Exception {
        CommandExecutor.Result result = new CommandExecutor().execute("definitely_missing_queuectl_command_987");

        assertThat(result.exitCode()).isNotZero();
    }
}
