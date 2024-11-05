---
description: Designing a simple experiment
---

# First Experiment
Now that you have downloaded OpenDC, we will start creating a simple experiment. 
In this experiment we will compare the performance of a small, and a big data center on the same workload.

:::info Learning goal
During this tutorial, we will learn how to create and execute a simple experiment in OpenDC.
:::

## Designing a Data Center

The first requirement to run an experiment in OpenDC is a **topology**. 
A **topology** defines the hardware on which a **workload** is executed. 
Larger topologies will be capable of running more workloads, and will often quicker. 

A **topology** is defined using a JSON file. A **topology** contains one or more _clusters_.
_clusters_ are groups of _hosts_ on a specific location. Each cluster consists of one or more _hosts_. 
A _host_ is a machine on which one or more tasks can be executed. _hosts_ are composed of a _cpu_ and a _memory_ unit. 

### Simple Data Center
in this experiment, we are comparing two data centers. Below is an example of the small **topology** file:

```json
{
    "clusters":
    [
        {
            "name": "C01",
            "hosts" :
            [
                {
                    "name": "H01",
                    "cpu":
                    {
                        "coreCount": 12,
                        "coreSpeed": 3300
                    },
                    "memory": {
                        "memorySize": 140457600000
                    }
                }
            ]
        }
    ]
}
```

This **topology** consist of a single _cluster_, with a single _host_. 

:::tip
To use this **topology** in experiment copy the content to a new JSON file, or download it [here](documents/topologies/small.json "download")
:::

### Simple Data Center
in this experiment, we are comparing two data centers. Below is an example of the bigger **topology** file:

```json
{
    "clusters":
    [
        {
            "name": "C01",
            "hosts" :
            [
                {
                    "name": "H01",
                    "cpu":
                    {
                        "coreCount": 32,
                        "coreSpeed": 3200
                    },
                    "memory": {
                        "memorySize": 256000
                    }
                }
            ]
        },
        {
            "name": "C02",
            "hosts" :
            [
                {
                    "name": "H02",
                    "count": 6,
                    "cpu":
                    {
                        "coreCount": 8,
                        "coreSpeed": 2930
                    },
                    "memory": {
                        "memorySize": 64000
                    }
                }
            ]
        },
        {
            "name": "C03",
            "hosts" :
            [
                {
                    "name": "H03",
                    "count": 2,
                    "cpu":
                    {
                        "coreCount": 16,
                        "coreSpeed": 3200
                    },
                    "memory": {
                        "memorySize": 128000
                    }
                }
            ]
        }
    ]
}
```

Compared to the small topology, the big topology consist of three clusters, all consisting of a single host.

:::tip
To use this **topology** in experiment copy the content to a new JSON file, or download it [here](documents/topologies/big.json "download")
:::

:::info
For more in depth information about Topologies, see [Topology](../documentation/Input/Topology)
:::

## Workloads

Next to the topology, we need a workload to simulate on the data center. 
In OpenDC, workloads are defined as a bag of tasks. Each task is accompanied by one or more fragments. 
These fragments define the computational requirements of the task over time. 
For this experiment, we will use the bitbrains-small workload. This is a small workload of 50 tasks, 
spanning over a bit more than a month time. You can download the workload [here](documents/workloads/bitbrains-small.zip "download")

:::info
For more in depth information about Workloads, see [Workload](../documentation/Input/Workload)
:::

## Executing an experiment

To run an experiment, we need to create an **experiment** file. This is a JSON file, that defines what should be executed 
by OpenDC, and how. Below is an example of a simple **experiment** file:

```json
{
    "name": "simple",
    "topologies": [{
        "pathToFile": "topologies/small.json"
    },
    {
        "pathToFile": "topologies/big.json"
    }],
    "workloads": [{
        "pathToFile": "traces/bitbrains-small",
        "type": "ComputeWorkload"
    }]
}
```

In this **experiment**, three things are defined. First, is the `name`. This defines how the experiment is called 
in the output folder. Second, is the `topologies`. This defines where OpenDC can find the topology files.
Finally, the `workloads`. This defines which workload OpenDC should run. You can download the experiment file [here](documents/experiments/simple_experiment.json "download")

As you can see, `topologies` defines two topologies. In this case OpenDC will run two simulations, one with the small
topology, and one with the big topology. 

:::info
For more in depth information about Experiments, see [Experiment](../documentation/Input/Experiment)
:::

## Running OpenDC
At this point, we should have all components to run an experiment. To make sure every file can be used by OpenDC, 
please create an experiment folder such as the one shown below:
```
── {simulation-folder-name} 📁 🔧
    ├── topologies 📁 🔒
    │   └── small.json 📄 🔧
    │   └── big.json 📄 🔧
    ├── experiments 📁 🔒
    │   └── simple_experiment.json 📄 🔧
    ├── workloads 📁 🔒
    │   └── bitbrains-small 📁 🔒
    │       └── fragments.parquet 📄 🔧
    │       └── tasks.parquet 📄 🔧
    ├── OpenDCExperimentRunner 📁 🔒
    │   └── lib 📁 🔒
    │   └── bin 📁 🔒
    ├── output 📁 🔒
```

Executing the experiment can be done directly from the terminal. 
Execute the following code from the terminal in simulation-folder-name

```
$ ./OpenDCExperimentRunner/bin/OpenDCExperimentRunner.sh --experiment-path "experiments/simple_experiment.json"
```
