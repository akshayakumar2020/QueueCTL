# QueueCTL

QueueCTL is a production-style CLI background job queue built with Java 21, Spring Boot 3, picocli, JdbcTemplate, and SQLite.

## Setup Instructions

```bash
mvn clean package
bin/queuectl --help
```

On Windows:

```powershell
mvn clean package
.\bin\queuectl.ps1 --help
```

If PowerShell script execution is restricted, use the Command Prompt launcher:

```powershell
.\bin\queuectl.cmd --help
```

You can always run the jar directly with `java -jar target/queuectl.jar --help`.

The SQLite database is created at `data/queuectl.db`.

## Usage Examples

Inline JSON works in POSIX shells:

```bash
bin/queuectl enqueue '{"id":"job1","command":"echo hello"}'
```

For shell-neutral automation, write JSON to a file and pass `@file`:

```powershell
.\bin\queuectl.ps1 enqueue '@data/job1.json'
enqueued job1
```

List pending jobs:

```text
ID    STATE    ATTEMPTS  RUN AT                          COMMAND
----  -------  --------  ------------------------------  -------------
job1  pending  0         2026-07-15T18:36:41.404571200Z  echo hello
job2  pending  0         2026-07-15T18:36:44.625431100Z  cmd /c exit 1
```

Configure retries/backoff:

```text
.\bin\queuectl.ps1 config set backoff-base 1
set backoff-base=1
```

Start workers:

```text
.\bin\queuectl.ps1 worker start --count 2
starting..
started workers: worker-d27e72e1, worker-0cc5e57f
```

After workers run:

```text
ID    STATE      ATTEMPTS  RUN AT                          COMMAND
----  ---------  --------  ------------------------------  -------------
job1  completed  0         2026-07-15T18:36:41.404571200Z  echo hello
job2  dead       2         2026-07-15T18:37:07.609849400Z  cmd /c exit 1
```

DLQ:

```text
.\bin\queuectl.ps1 dlq list
ID    ATTEMPTS  ERROR     COMMAND
----  --------  --------  -------------
job2  2         exit 1:   cmd /c exit 1

.\bin\queuectl.ps1 dlq retry job2
requeued job2
```

Status:

```text
Queue status
STATE       COUNT
----------  -----
pending     0
processing  0
completed   1
failed      0
dead        1
active workers: 2
```

Stop workers:

```text
.\bin\queuectl.ps1 worker stop
signaled workers: 2
```

Malformed JSON fails without a stack trace:

```text
invalid job: Unexpected character ('b' (code 98)): was expecting double-quote to start field name
```

## Architecture Overview

Jobs move through `pending -> processing -> completed`, or on failure back to `pending` until retry budget is exhausted, then `dead`.

SQLite is used because the assignment needs a lightweight durable store shared across separate worker JVMs. Plain JDBC with `JdbcTemplate` is deliberate: the claim operation needs explicit SQL control. JPA/Hibernate would add caching and unit-of-work behavior that is unnecessary here and can obscure the atomic row update.

Duplicate processing is prevented by a guarded claim update:

```sql
UPDATE jobs
SET state = 'processing', worker_id = ?, updated_at = ?
WHERE id = ? AND state = 'pending'
```

Only the worker whose update affects one row owns the job. Workers communicate only through SQLite, with WAL mode and a busy timeout enabled for concurrent access.

## Assumptions & Trade-offs

The default database path is local to the working directory. `dlq retry` resets attempts to `0`, which makes manual recovery simple and predictable. Worker stop uses Java `ProcessHandle.destroy()`, which is graceful on normal platforms, and each worker’s shutdown hook stops polling after the in-flight command finishes.

The CLI supports `@file` JSON payloads because Windows native command quoting can strip embedded JSON quotes. Picocli at-file expansion is disabled so QueueCTL can own this behavior.

## Testing Instructions

Run unit tests:

```bash
mvn test
```

Run the full package build:

```bash
mvn clean package
```

Run the end-to-end shell test:

```bash
bash scripts/integration-test.sh
```

Run the Windows PowerShell end-to-end test:

```powershell
.\scripts\integration-test.ps1
```

The JUnit suite verifies job insert/read, concurrent atomic claim, malformed JSON handling, command execution success/failure, and retry-to-DLQ behavior. The shell and PowerShell scripts build the jar, enqueue jobs through the CLI, start real worker JVMs, verify completion and DLQ behavior, and stop workers.
