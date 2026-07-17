# QueueCTL Design

QueueCTL is a production-style command-line background job queue. It is built with Java 21, Spring Boot 3, picocli, JdbcTemplate, and SQLite. The application focuses on durable local job storage, multiple worker processes, retry handling, and a simple CLI interface.

For setup and usage examples, see [README.md](README.md).

## Goals

- Provide a small CLI for enqueueing, listing, processing, retrying, and inspecting jobs.
- Store queue state durably so separate CLI and worker JVMs can share the same queue.
- Prevent duplicate processing when multiple workers poll at the same time.
- Keep the implementation easy to inspect by using explicit SQL instead of a heavier ORM.
- Work well on Windows and POSIX shells through launcher scripts and `@file` JSON input.

## Main Components

`QueuectlApplication` starts Spring Boot and delegates command execution to picocli.

The `cli` package contains the user-facing commands:

- `enqueue` validates a JSON payload and creates a job.
- `list` shows jobs, optionally filtered by state.
- `worker start` launches one or more worker JVMs.
- `worker stop` signals active worker processes.
- `status` shows job counts and active worker count.
- `dlq list` and `dlq retry` manage dead jobs.
- `config get` and `config set` read or update queue settings.
- `internal-worker-run` is a hidden command used as the worker process entrypoint.

The `core` package owns the queue model, repositories, configuration, and service logic. `QueueService` coordinates validation, enqueueing, claiming, completion, failure, retry, and dead-letter transitions.

The `worker` package owns process launching and job execution. `WorkerLauncher` starts and stops worker JVMs. `WorkerRunner` polls for due work and executes commands through `CommandExecutor`.

## Persistence Model

SQLite is the shared durable store. The default database path is `data/queuectl.db`.

The schema has three main tables:

- `jobs`: stores command payloads, state, retry counters, timestamps, worker ownership, output, and last error.
- `workers`: stores worker IDs, process IDs, state, and timestamps.
- `config`: stores runtime settings such as `max-retries` and `backoff-base`.

SQLite WAL mode and a busy timeout are configured so multiple short-lived CLI processes and worker JVMs can access the same database with fewer lock conflicts.

## Job Lifecycle

Jobs normally move through this flow:

```text
pending -> processing -> completed
```

When command execution fails, the job is stored as `failed` until its next `run_at` time, then reclaimed as `processing` until its retry budget is exhausted:

```text
pending -> processing -> failed -> processing -> dead
```

`failed` jobs are still eligible for processing after their `run_at` timestamp is reached. `dead` jobs are no longer processed automatically and must be manually requeued with `dlq retry`.

## Enqueue Flow

`enqueue` accepts JSON with at least:

```json
{
  "id": "job1",
  "command": "echo hello"
}
```

The command validates required fields, applies `max_retries` from the payload or config, and inserts a `pending` job with `run_at` set to the current time.

The CLI also supports `@file` payloads. This avoids fragile quoting for inline JSON, especially on Windows shells.

## Claiming and Concurrency

Workers claim jobs using a two-step SQL flow:

1. Select the next due job where `state` is `pending` or `failed`.
2. Update that row to `processing` only if it is still claimable.

The guarded update is the important concurrency control:

```sql
UPDATE jobs
SET state = 'processing', worker_id = ?, updated_at = ?
WHERE id = ? AND state IN ('pending', 'failed')
```

Only the worker whose update affects one row owns the job. If another worker claimed the same candidate first, the update affects zero rows and the worker returns to polling.

## Worker Execution

`worker start --count N` starts N separate Java processes running the hidden `internal-worker-run` command. Each worker:

- registers itself in the `workers` table;
- polls for due jobs;
- claims one job at a time;
- executes the command with `ProcessBuilder`;
- stores command output on success;
- stores a compact error message on failure;
- marks itself stopped during shutdown.

Worker logs are written under `data/workers/`.

## Retry and Dead Letter Queue

On failure, `QueueService` increments the job attempt count.

If the new attempt count is less than `max_retries`, the job is marked `failed` and scheduled for a future `run_at`. The delay is calculated from `backoff-base` and the attempt number.

If the retry budget is exhausted, the job is marked `dead`. Dead jobs can be inspected with `dlq list` and returned to the queue with `dlq retry`, which resets attempts to `0`.

## Configuration

Runtime config is stored in the database so separate CLI invocations and worker processes see the same values.

Default values:

- `max-retries`: `3`
- `backoff-base`: `2`

These can be changed through:

```powershell
.\bin\queuectl.ps1 config get max-retries
.\bin\queuectl.ps1 config set max-retries 5
.\bin\queuectl.ps1 config set backoff-base 2
```

## Shell Launchers

The project provides launchers for common environments:

- `bin/queuectl` for POSIX shells.
- `bin/queuectl.ps1` for PowerShell.
- `bin/queuectl.cmd` for Windows Command Prompt.
- `demo.ps1` is an optional helper that prints sample commands.

## Trade-offs

SQLite keeps the project simple and portable, but it is intended for local or lightweight queue usage rather than high-throughput distributed workloads.

JdbcTemplate is used instead of JPA/Hibernate because the queue depends on explicit claim and update behavior. Direct SQL makes the concurrency behavior easier to reason about.

Worker processes communicate only through SQLite. This keeps the architecture simple, but it means worker supervision is basic and tied to local process IDs.

If a worker process is killed abruptly while a job is processing, that job has no automatic re-claim path and will remain stuck until manually requeued. This was accepted as an out-of-scope edge case for a local CLI queue.

Command output and error values are trimmed before storage to keep the database compact.

## Testing Strategy

Unit tests cover repository behavior, queue service transitions, command execution success and failure, malformed JSON handling, concurrent claim behavior, and retry-to-DLQ behavior.

Integration scripts exercise the real CLI, database, worker processes, job completion, DLQ behavior, and worker shutdown:

```bash
bash scripts/integration-test.sh
```

```powershell
.\scripts\integration-test.ps1
```
