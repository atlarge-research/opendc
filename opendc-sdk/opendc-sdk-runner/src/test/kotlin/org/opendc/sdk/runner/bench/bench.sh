#!/usr/bin/env bash
# Runs ExportOverheadMeasurement once per mode, each in its own test JVM, in the given order.
#
# Usage: [BENCH_PIN=<cpus>] bench.sh <trace> <mode>...
#   e.g. BENCH_PIN=0-11 bench.sh borg_3days none discard parquet none discard parquet
#
# BENCH_PIN pins Gradle to the given CPUs (taskset) and runs it without its daemon, so the test JVM inherits the CPU
# set. On a CPU with performance and efficiency cores, pin to the performance cores (0-11 on a Core Ultra 7 155H), or
# the timings vary with the cores the threads land on. Results are appended to opendc-sdk-runner/build/export-bench.csv.
set -euo pipefail

cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
trace=$1
shift

pin=()
gradle_args=()
if [ -n "${BENCH_PIN:-}" ]; then
  pin=(taskset -c "$BENCH_PIN")
  gradle_args=(--no-daemon)
fi

for mode in "$@"; do
  echo "=== $trace $mode $(date +%T)"
  OPENDC_EXPORT_BENCH=$mode OPENDC_BENCH_TRACE=$trace "${pin[@]}" ./gradlew "${gradle_args[@]}" \
    :opendc-sdk:opendc-sdk-runner:test --tests '*ExportOverheadMeasurement*' --rerun -q 2>&1 |
    grep -v "^\s*at \|WARNING\|SLF4J" | tail -5 || true
done

echo "=== done $(date +%T)"
cat opendc-sdk/opendc-sdk-runner/build/export-bench.csv
