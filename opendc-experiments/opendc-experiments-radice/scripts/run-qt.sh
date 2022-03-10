#!/bin/sh
REPEATS=${REPEATS:-1000000}

bin/radice qt --arrival-rate 4 --service-rate 1 --servers 5 -n "$REPEATS"
bin/radice qt --arrival-rate 4 --service-rate 1 --servers 6 -n "$REPEATS"
bin/radice qt --arrival-rate 4 --service-rate 1 --servers 10 -n "$REPEATS"
bin/radice qt --arrival-rate 28 --service-rate 3 --servers 10 -n "$REPEATS"
bin/radice qt --arrival-rate 28 --service-rate 3 --servers 12 -n "$REPEATS"
bin/radice qt --arrival-rate 16 --service-rate 0.75 --servers 22 -n "$REPEATS"
bin/radice qt --arrival-rate 16 --service-rate 0.75 --servers 24 -n "$REPEATS"
