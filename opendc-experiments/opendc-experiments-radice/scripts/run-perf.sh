#!/bin/sh
bin/radice-perf -r 32 --trace baseline --topology base --engine opendc \
                -P workload=baseline -P topology=baseline
bin/radice-perf -r 4 --trace baseline --topology base --engine cloudsim-plus \
                -P workload=baseline -P topology=baseline

bin/radice-perf -r 32 --trace baseline-50% --topology base --topology-scale 0.5 --engine opendc \
                -P workload=baseline-50% -P topology=baseline-50%
bin/radice-perf -r 8 --trace baseline-50% --topology base --topology-scale 0.5 --engine cloudsim-plus \
                -P workload=baseline-50% -P topology=baseline-50%

bin/radice-perf -r 32 --trace baseline-25% --topology base --topology-scale 0.25 --engine opendc \
                -P workload=baseline-25% -P topology=baseline-25%
bin/radice-perf -r 16 --trace baseline-25% --topology base --topology-scale 0.25 --engine cloudsim-plus \
                -P workload=baseline-25% -P topology=baseline-25%

bin/radice-perf -r 32 --trace baseline-10% --topology base --topology-scale 0.1 --engine opendc \
                -P workload=baseline-10% -P topology=baseline-10%
bin/radice-perf -r 32 --trace baseline-10% --topology base --topology-scale 0.1 --engine cloudsim-plus \
                -P workload=baseline-10% -P topology=baseline-10%
