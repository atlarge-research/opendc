---
description: The terms OpenDC uses for what it simulates, how it simulates, and what it reports.
---

# Concepts

This page defines the terms that the rest of the documentation uses. It first describes what OpenDC simulates, then
how an experiment turns into simulations, and finally what a simulation reports.

## What OpenDC simulates

OpenDC is a *discrete-event simulator*: it keeps a virtual clock and advances it from one event to the next, such as
the arrival of a task or the failure of a host. A simulation therefore covers weeks of datacenter operation in
seconds or minutes of real time. All times in an experiment and in its results are *simulated time*, measured on this
virtual clock.

A simulation combines a description of hardware, a description of work and the policies that bring the two together.

### Hardware

A *topology* describes the hardware. It consists of one or more *datacenters*. A datacenter has a *power source*,
optionally a *battery*, and one or more *clusters*. A cluster is a group of *hosts*, and a host is a machine with a
CPU, memory and optionally a GPU.

Each host has a *power model*, which determines the power it draws from its utilisation. The power source of a
datacenter can have a *carbon trace*, which states how much carbon the production of its energy emits over time. From
these two, OpenDC computes the energy use and the carbon emissions of a simulation.

### Work

A *workload* is the set of *tasks* that the datacenter receives. A task stands for a unit of work that occupies a host
for some time, such as a virtual machine or a batch job. It has a submission time, a duration and resource requirements:
a number of CPU cores, an amount of memory and optionally a GPU.

The demand of a task varies while it runs. A task therefore consists of *fragments*: consecutive periods during each of
which its CPU and GPU usage is constant. A workload is either written out in the experiment or read from a *trace*,
which is a set of Parquet files recorded from a real system.

### Policies and models

Four kinds of settings determine how hardware and work interact:

allocation policy
:   decides on which host a task runs, and when. The scheduler of the simulated datacenter applies it to every task.

failure model
:   decides when hosts fail, for how long, and how many. A failure interrupts the tasks on a host.

checkpoint model
:   lets tasks save their progress periodically, so that a failure loses less work.

export model
:   decides which results a simulation writes, and how often.

## From experiment to runs

An *experiment* is the unit that a user writes and runs. It lists candidate values for each of its parts: one or more
topologies, one or more workloads, one or more allocation policies and so on.

A *scenario* is one combination of these values: one topology with one workload, one allocation policy, one failure
model, one checkpoint model and one export model. An experiment has one scenario for every possible combination. An
experiment with two topologies, one workload and three allocation policies has six scenarios.

Failure models and the sampling of workloads are random. To average over this randomness, an experiment can simulate
each scenario several times with different *seeds* for the random-number generator. A *run* is the simulation of one
scenario with one seed. Figure 1 shows the relation between the three terms.

``` mermaid
flowchart LR
    experiment["Experiment<br>2 topologies, 1 workload,<br>2 allocation policies, 2 runs each"]
    experiment --> s0["Scenario 0"]
    experiment --> s1["Scenario 1"]
    experiment --> s2["Scenario 2"]
    experiment --> s3["Scenario 3"]
    s0 --> r00["Run, seed 0"]
    s0 --> r01["Run, seed 1"]
    s3 --> r30["Run, seed 0"]
    s3 --> r31["Run, seed 1"]
```

/// figure-caption
An experiment expands into scenarios, and every scenario into runs. The runs of scenarios 1 and 2 are left out.
///

Runs are independent of each other. This is why OpenDC can simulate several runs at the same time, and why a server
can distribute the runs of one experiment over a cluster.

## What a simulation reports

During a run, OpenDC takes a *sample* of the state of the datacenter at a fixed interval of simulated time, the
*export interval*. A sample records, for example, the utilisation and power draw of every host and the number of tasks
that wait, run and have finished.

The samples of a run form its *output*: a set of tables with one row per sample and entity. On disk, each table is a
Parquet file, such as `host.parquet`. The [output reference](../reference/output/index.md) lists the tables and their
columns.

## Summary of terms

| Term              | Meaning                                                                       |
|-------------------|-------------------------------------------------------------------------------|
| topology          | the hardware: datacenters, clusters and hosts                                 |
| workload          | the tasks that a datacenter receives                                          |
| task              | a unit of work that occupies a host for some time                             |
| fragment          | a period during which the demand of a task is constant                        |
| trace             | recorded data in Parquet files: a workload, carbon intensities or failures    |
| allocation policy | the rule that places tasks on hosts                                           |
| experiment        | candidate topologies, workloads, policies and models to compare               |
| scenario          | one combination of the candidates of an experiment                            |
| run               | the simulation of one scenario with one seed                                  |
| sample            | the recorded state of the datacenter at one moment of simulated time          |
| output            | the tables of samples that a run produces                                     |
