#!/bin/sh
REPEATS=${REPEATS:-4096}
PARALLELISM=${PARALLELISM:-80}

bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/baseline.yml -P portfolio=baseline
bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/phenomena.yml -P portfolio=phenomena
bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/topology-opt.yml -P portfolio=topology-opt
bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/workload.yml -P portfolio=workload
bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/workload-opt.yml -P portfolio=workload-opt
bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/scheduler.yml -P portfolio=scheduler
bin/radice run -r "$REPEATS" -p "$PARALLELISM" portfolios/scheduler-opt.yml -P portfolio=scheduler-opt
