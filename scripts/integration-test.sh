#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mvn -q clean package
rm -rf data
mkdir -p data

FAIL_COMMAND="sh -c 'exit 1'"
if [[ "${OS:-}" == "Windows_NT" ]]; then
  FAIL_COMMAND="cmd /c exit 1"
fi

cat > data/e2e-ok.json <<JSON
{"id":"e2e-ok","command":"echo e2e-ok"}
JSON

cat > data/e2e-dead.json <<JSON
{"id":"e2e-dead","command":"$FAIL_COMMAND","max_retries":2}
JSON

java -jar target/queuectl.jar config set backoff-base 1
java -jar target/queuectl.jar enqueue @data/e2e-ok.json
java -jar target/queuectl.jar enqueue @data/e2e-dead.json
java -jar target/queuectl.jar worker start --count 2
sleep 5
java -jar target/queuectl.jar worker stop

LIST_OUTPUT="$(java -jar target/queuectl.jar list)"
DLQ_OUTPUT="$(java -jar target/queuectl.jar dlq list)"

echo "$LIST_OUTPUT"
echo "$DLQ_OUTPUT"

grep -q "e2e-ok  completed" <<< "$LIST_OUTPUT"
grep -q "e2e-dead  dead" <<< "$LIST_OUTPUT"
grep -q "e2e-dead" <<< "$DLQ_OUTPUT"

echo "integration test passed"
