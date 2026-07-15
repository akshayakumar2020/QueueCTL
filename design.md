# QueueCTL Design

QueueCTL is a Spring Boot command-line application with picocli commands over a SQLite-backed queue.

## Core Flow

1. `enqueue` validates JSON and inserts a `pending` job.
2. `worker start` spawns separate JVM processes running `internal-worker-run`.
3. Each worker polls SQLite for due `pending` jobs.
4. Claiming uses a guarded update: a worker first finds a candidate, then updates it to `processing` only if the row is still `pending`.
5. The worker executes the command via `ProcessBuilder`.
6. Exit code `0` marks `completed`; failures either reschedule the job with exponential backoff or mark it `dead`.

## Persistence

The database lives at `data/queuectl.db` by default. SQLite WAL mode and a busy timeout are enabled for safer concurrent access from multiple worker JVMs.

## Retry Policy

`max-retries` and `backoff-base` are stored in the `config` table. Delay is calculated as `base ^ attempts` seconds. A dead job can be requeued with `dlq retry`, which resets attempts to `0`.

## Shutdown

Workers install a JVM shutdown hook. On graceful termination they stop polling, finish the current command, and mark themselves stopped.
