CREATE TABLE IF NOT EXISTS jobs (
    id TEXT PRIMARY KEY,
    command TEXT NOT NULL,
    state TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    run_at TEXT NOT NULL,
    worker_id TEXT,
    last_error TEXT,
    output TEXT
);

CREATE INDEX IF NOT EXISTS idx_jobs_state_run_at ON jobs(state, run_at, created_at);

CREATE TABLE IF NOT EXISTS config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS workers (
    id TEXT PRIMARY KEY,
    pid INTEGER NOT NULL,
    state TEXT NOT NULL,
    started_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

INSERT OR IGNORE INTO config(key, value, updated_at) VALUES('max-retries', '3', datetime('now'));
INSERT OR IGNORE INTO config(key, value, updated_at) VALUES('backoff-base', '2', datetime('now'));
