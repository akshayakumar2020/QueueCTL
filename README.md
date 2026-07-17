# QueueCTL

A production-style CLI-based background job queue built with **Java 21**, **Spring Boot 3**, **Picocli**, **Spring JdbcTemplate**, and **SQLite**.

QueueCTL provides a lightweight and durable background job processing system capable of managing multiple worker processes, retrying failed jobs with configurable exponential backoff, maintaining a Dead Letter Queue (DLQ), and persisting jobs across application restarts.

---

# 🎥 Demo

> **Watch QueueCTL in action:**

**Google Drive:**
https://drive.google.com/file/d/1Vw_R0yS3VpDBT517NwZwPoGreI-e8Sz0/view?usp=sharing

---

# ✨ Features

* ✅ Windows-friendly CLI using PowerShell and CMD launchers
* ✅ Persistent SQLite-backed job queue
* ✅ Concurrent worker processes
* ✅ Atomic job claiming to prevent duplicate execution
* ✅ Configurable retry count with exponential backoff
* ✅ Dead Letter Queue (DLQ)
* ✅ Graceful worker shutdown
* ✅ Automatic database initialization
* ✅ JSON file (`@file`) support for reliable Windows execution
* ✅ Unit tests and end-to-end integration tests

---

# 🛠️ Technology Stack

| Technology          | Purpose                |
| ------------------- | ---------------------- |
| Java 21             | Programming Language   |
| Spring Boot 3       | Application Framework  |
| Picocli             | Command Line Interface |
| Spring JdbcTemplate | Database Access        |
| SQLite              | Persistent Storage     |
| Maven               | Build Tool             |
| JUnit 5             | Testing                |

---

# 📋 Requirements

Before running the project, install:

* Java 21 (JDK)
* Maven 3.8+
* Windows PowerShell (recommended)

No external database is required.

SQLite is bundled, and the database schema is automatically created during the first run.

---

# 📥 Installation

Clone the repository and build the project.

```powershell
git clone https://github.com/akshayakumar2020/QueueCTL.git
mvn clean package
```



---

# 🔨 Build

If you've already cloned the repository, simply run:

```powershell
mvn clean package
```

---

# 📁 Project Structure

```text
FLAM/
│
├── bin/
│   ├── queuectl
│   ├── queuectl.cmd
│   └── queuectl.ps1
│
├── data/
│
├── scripts/
│   ├── integration-test.ps1
│   └── integration-test.sh
│
├── src/
│   ├── main/
│   └── test/
│
├── target/
│
├── .gitignore
├── demo.ps1
├── design.md
├── pom.xml
└── README.md
```

---

# 🚀 Running QueueCTL

Display the available commands.

```powershell
.\bin\queuectl.ps1 --help
```

Alternatively, run the JAR directly.

```powershell
java -jar target/queuectl.jar --help
```

The SQLite database is automatically created at:

```text
data/queuectl.db
```

---

# ⚡ Quick Reference (Windows)

Run the following commands in order.

```powershell
mvn clean package

.\bin\queuectl.ps1 enqueue "@data/job1.json"

.\bin\queuectl.ps1 enqueue "@data/job2.json"

.\bin\queuectl.ps1 list

.\bin\queuectl.ps1 config set backoff-base 1

.\bin\queuectl.ps1 worker start --count 2

.\bin\queuectl.ps1 list

.\bin\queuectl.ps1 status

.\bin\queuectl.ps1 dlq list

.\bin\queuectl.ps1 dlq retry job2

.\bin\queuectl.ps1 worker stop

mvn test

.\scripts\integration-test.ps1
```

---

# 💡 Usage Examples

## Enqueue a Job

Create a JSON file.

**data/job1.json**

```json
{
  "id": "job1",
  "command": "echo hello"
}
```

Run:

```powershell
.\bin\queuectl.ps1 enqueue "@data/job1.json"
```

Output:

```text
enqueued job1
```

---

## List Jobs

```powershell
.\bin\queuectl.ps1 list
```

Example output:

```text
ID        STATE      ATTEMPTS   RUN AT                          COMMAND
--------  ---------  ---------  ------------------------------  ----------------
job1      pending    0          2026-07-15T18:36:41Z           echo hello
job2      pending    0          2026-07-15T18:36:44Z           cmd /c exit 1
```

---

## Configure Retry Backoff

```powershell
.\bin\queuectl.ps1 config set backoff-base 1
```

Output:

```text
set backoff-base=1
```

---

## Start Workers

```powershell
.\bin\queuectl.ps1 worker start --count 2
```

Example output:

```text
starting...
started workers:
worker-d27e72e1
worker-0cc5e57f
```

---

## After Processing

```powershell
.\bin\queuectl.ps1 list
```

```text
ID        STATE       ATTEMPTS   RUN AT                          COMMAND
--------  ----------  ---------  ------------------------------  ----------------
job1      completed   0          2026-07-15T18:36:41Z           echo hello
job2      dead        2          2026-07-15T18:37:07Z           cmd /c exit 1
```

---

## Dead Letter Queue (DLQ)

List failed jobs.

```powershell
.\bin\queuectl.ps1 dlq list
```

```text
ID        ATTEMPTS   ERROR      COMMAND
--------  ---------  ---------  ----------------
job2      2          exit 1     cmd /c exit 1
```

Retry a dead job.

```powershell
.\bin\queuectl.ps1 dlq retry job2
```

Output:

```text
requeued job2
```

---

## Queue Status

```powershell
.\bin\queuectl.ps1 status
```

Example:

```text
Queue Status

STATE         COUNT
------------  -----
pending       0
processing    0
completed     1
failed        0
dead          1

Active Workers: 2
```

---

## Stop Workers

```powershell
.\bin\queuectl.ps1 worker stop
```

Output:

```text
signaled workers: 2
```

---

## Invalid JSON

Malformed JSON is handled gracefully.

```text
invalid job:
Unexpected character ('b' (code 98))
```

No Java stack trace is displayed.

---

# 🏗️ Architecture Overview

QueueCTL follows the lifecycle below.

```text
                   Enqueue Job
                        │
                        ▼
                 +---------------+
                 |    Pending    |
                 +---------------+
                        │
                 Worker Claims Job
                        ▼
                 +---------------+
                 |  Processing   |
                 +---------------+
                  │            │
             Success        Failure
                  │            │
                  ▼            ▼
          +---------------+   Retry?
          |  Completed    |     │
          +---------------+     │
                                │
                              Yes
                                │
                                ▼
                            Pending
                                │
                        Retry Limit Reached
                                ▼
                        +---------------+
                        |     Dead      |
                        +---------------+
                                │
                           DLQ Retry
                                ▼
                             Pending
```

---

# 🔄 Concurrency Design

To prevent duplicate processing, workers claim jobs using an atomic SQL update.

```sql
UPDATE jobs
SET state='processing',
    worker_id=?,
    updated_at=?
WHERE id=?
AND state='pending';
```

Only the worker that successfully updates one row owns the job.

SQLite runs in **Write-Ahead Logging (WAL)** mode with a configured busy timeout, allowing multiple worker JVMs to coordinate safely using the same database.

---

# 🤔 Design Decisions

## Why SQLite?

* Lightweight
* Zero configuration
* Persistent storage
* Shared by multiple worker processes

## Why Spring JdbcTemplate?

Atomic job claiming requires explicit SQL control.

Using JdbcTemplate keeps the implementation simple while avoiding ORM caching and persistence context overhead introduced by JPA/Hibernate.

---

# ⚖️ Assumptions & Trade-offs

* Jobs are stored locally in `data/queuectl.db`.
* Workers communicate exclusively through SQLite.
* `dlq retry` resets the retry count to `0`.
* Worker shutdown is graceful and waits for the current job to finish.
* Windows users are encouraged to use `@file` JSON payloads because PowerShell command quoting differs from POSIX shells.

---

# ✅ Testing

Run unit tests.

```powershell
mvn test
```

Build the project.

```powershell
mvn clean package
```

Run the Windows integration test.

```powershell
.\scripts\integration-test.ps1
```

The test suite verifies:

* Job enqueue
* Job retrieval
* Atomic concurrent job claiming
* Command execution
* Retry mechanism
* Dead Letter Queue
* Invalid JSON handling
* End-to-end CLI workflow

```

## 👨‍💻 Author

**Akshaya Kumar**

- GitHub: [akshayakumar2020](https://github.com/akshayakumar2020)
- LinkedIn: [Akshaya Kumar](https://www.linkedin.com/in/your-linkedin-profile/)
