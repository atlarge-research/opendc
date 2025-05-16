When using OpenDC, an experiment defines what should be run, and how. An experiment consists of one or more scenarios, 
each defining a different simulation to run. Scenarios can differ in many things, such as the topology that is used, 
the workload that is run, or the policies that are used to name a few. An experiment is defined using a JSON file. 
In this page, we will discuss how to properly define experiments for OpenDC.

:::info Code
All code related to reading and processing Experiment files can be found [here](https://github.com/atlarge-research/opendc/tree/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/experiment)
The code used to run experiments can be found [here](https://github.com/atlarge-research/opendc/tree/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/runner)
:::

## Schema

In the following section, we describe the different components of an experiment. Following is a table with all experiment components:

| Variable           | Type                                                                 | Required? | Default       | Description                                                                                           |
|--------------------|----------------------------------------------------------------------|-----------|---------------|-------------------------------------------------------------------------------------------------------|
| name               | string                                                               | no        | ""            | Name of the scenario, used for identification and referencing.                                        |
| outputFolder       | string                                                               | no        | "output"      | Directory where the simulation outputs will be stored.                                                |
| runs               | integer                                                              | no        | 1             | Number of times the same scenario should be run. Each scenario is run with a different seed.          |
| initialSeed        | integer                                                              | no        | 0             | The seed used for random number generation during a scenario. Setting a seed ensures reproducability. |
| topologies         | List[path/to/file]                                                   | yes       | N/A           | Paths to the JSON files defining the topologies.                                                      |
| workloads          | List[[Workload](/docs/documentation/Input/Workload)]                 | yes       | N/A           | Paths to the files defining the workloads executed.                                                   |
| allocationPolicies | List[[AllocationPolicy](/docs/documentation/Input/AllocationPolicy)] | yes       | N/A           | Allocation policies used for resource management in the scenario.                                     |
| failureModels      | List[[FailureModel](/docs/documentation/Input/FailureModel)]         | no        | List[null]    | List of failure models to simulate various types of failures.                                         |
| maxNumFailures     | List[integer]                                                        | no        | [10]          | The max number of times a task can fail before being terminated.                                      |
| checkpointModels   | List[[CheckpointModel](/docs/documentation/Input/CheckpointModel)]   | no        | List[null]    | Paths to carbon footprint trace files.                                                                |
| exportModels       | List[[ExportModel](/docs/documentation/Input/ExportModel)]           | no        | List[default] | Specifications for exporting data from the simulation.                                                |

Most components of an experiment are not single values, but lists of values.
This allows users to run multiple scenarios using a single experiment file.
OpenDC will generate and execute all permutations of the different values.

Some of the components in an experiment file are paths to files, or complicated objects. The format of these components 
are defined in their respective pages.

## Examples
In the following section, we discuss several examples of experiment files.

### Simple

The simplest experiment that can be provided to OpenDC is shown below:
```json
{
    "topologies": [
        {
            "pathToFile": "topologies/topology1.json"
        }
    ],
    "workloads": [
        {
            "type": "ComputeWorkload",
            "pathToFile": "traces/bitbrains-small"
        }
    ],
    "allocationPolicies": [
        {
            "type": "prefab",
            "policyName": "Mem"
        }
    ]
}
```

This experiment creates a simulation from file topology1, located in the topologies folder, with a workload trace from the
bitbrains-small file, and an allocation policy of type Mem. The simulation is run once (by default), and the default
name is "".

### Complex
Following is an example of a more complex experiment:
```json
{
    "topologies": [
        {
            "pathToFile": "topologies/topology1.json"
        },
        {
            "pathToFile": "topologies/topology2.json"
        },
        {
            "pathToFile": "topologies/topology3.json"
        }
    ],
    "workloads": [
        {
            "pathToFile": "traces/bitbrains-small",
            "type": "ComputeWorkload"
        },
        {
            "pathToFile": "traces/bitbrains-large",
            "type": "ComputeWorkload"
        }
    ],
    "allocationPolicies": [
        {
            "type": "prefab",
            "policyName": "Mem"
        },
        {
            "type": "prefab",
            "policyName": "Mem-Inv"
        }
    ]
}
```

This scenario runs a total of 12 experiments. We have 3 topologies (3 datacenter configurations), each simulated with
2 distinct workloads, each using a different allocation policy (either Mem or Mem-Inv).
