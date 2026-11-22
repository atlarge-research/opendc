---
description: Documentation of OpenDC 2, which is no longer developed but still maintained.
status: deprecated
search:
  exclude: true
---

# OpenDC 2 Documentation

OpenDC 2 is the predecessor of OpenDC 3. It is no longer developed, but it is maintained on the
[`OpenDC2-maintenance`](https://github.com/atlarge-research/opendc/tree/OpenDC2-maintenance) branch. The pages of this
section document the formats of OpenDC 2: the experiment file, the topology, the traces and the output files.

OpenDC 3 can still run most OpenDC 2 experiment files with [`opendc --legacy run`](../reference/cli.md). The
[migration page](migrating.md) explains this option and how to move an experiment to the
[format of OpenDC 3](../reference/experiment/index.md).

The search of this site excludes the pages of this section. The section has the following pages:

- [Running OpenDC 2](running.md) explains how to obtain OpenDC 2 and how to run an experiment with it.
- [Experiment File](experiment.md) describes the experiment file and its failure, checkpoint and export models.
- [Topology](topology.md) describes the topology file, with its clusters, hosts, power sources and batteries.
- [Workload](workload.md) describes workloads and the format of a workload trace.
- [Allocation Policy](allocation-policy.md) describes the policies that decide on which host each task runs.
- [Output](output.md) describes where OpenDC 2 writes its results and lists the columns of each output file.
- [Units](units.md) lists the units of the input values.
- [M3SA](m3sa.md) describes Multi-Meta-Model Simulation Analysis, which combines the results of several scenarios.
