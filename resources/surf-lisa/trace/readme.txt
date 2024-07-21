energy.parquet -- ground truth for the energy
    - sampled every 30s

meta.parquet
    - all the tasks that need to be executed (7850 tasks)

trace.parquet
    - the fragments of the tasks (within a task, the CPU utilization varies)
    - cpu_usage tells the MHz used, on average, during the last 30s
    - was taken from surf lisa public data from SURF



background of the data:
1. we selected nodes with only CPUs
2. we selected only jobs that run on single nodes (aka not distributed jobs) and no shared nodes
3. between oct 7th - oct 14th (aka 1 week) from SURF Lisa datacenters


