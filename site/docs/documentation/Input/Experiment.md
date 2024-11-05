When using OpenDC, an experiment defines what should be run, and how. An experiment consists of one or more scenarios, 
each defining a different simulation to run. Scenarios can differ in many things, such as the topology that is used, 
the workload that is run, or the policies that are used to name a few. An experiment is defined using a JSON file. 
In this page, we will discuss how to properly define experiments for OpenDC.

:::info Code
All code related to reading and processing Experiment files can be found [here](https://github.com/atlarge-research/opendc/tree/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/experiment)

The code used to run a given experiment can be found [here](https://github.com/atlarge-research/opendc/tree/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/runner)
:::

## Schema

The schema for the scenario file is provided in [schema](ExperimentSchema)
In the following section, we describe the different components of the schema.
Some components of an experiment are not single values, but lists. This is used to run multiple scenarios using 
a single experiment file. OpenDC will execute all permutations of the different values. 
This means that if all list based values have a single value, only one Scenario will be run. 

| Variable            | Type                                         | Required? | Default  | Description                                                       |
|---------------------|----------------------------------------------|-----------|----------|-------------------------------------------------------------------|
| name                | string                                       | no        | ""       | Name of the scenario, used for identification and referencing.    |
| outputFolder        | string                                       | no        | "output" | Directory where the simulation outputs will be stored.            |
| initialSeed         | integer                                      | no        | 0        | Seed used for random number generation to ensure reproducibility. |
| runs                | integer                                      | no        | 1        | Number of times the scenario should be run.                       |
| exportModels        | List[[ExportModel](#exportmodel)]            | no        | Default  | Specifications for exporting data from the simulation.            |
| computeExportConfig | [ComputeExportConfig](#checkpointmodel)      | no        | Default  | The features that should be exported during the simulation        |
| maxNumFailures      | List[integer]                                | no        | [10]     | The max number of times a task can fail before being terminated.  |
| topologies          | List[[Topology](#topology)]                  | yes       | N/A      | List of topologies used in the scenario.                          |
| workloads           | List[[Workload](#workload)]                  | yes       | N/A      | List of workloads to be executed within the scenario.             |
| allocationPolicies  | List[[AllocationPolicy](#allocation-policy)] | yes       | N/A      | Allocation policies used for resource management in the scenario. |
| failureModels       | List[[FailureModel](#failuremodel)]          | no        | Default  | List of failure models to simulate various types of failures.     |
| checkpointModels    | List[[CheckpointModel](#checkpointmodel)]    | no        | null     | Paths to carbon footprint trace files.                            |
| carbonTracePaths    | List[string]                                 | no        | null     | Paths to carbon footprint trace files.                            |


Many of the input fields of the experiment file are complex objects themselves. Next, we will describe the required input 
type of each of these fields.

### ExportModel

| Variable       | Type  | Required? | Default | Description                                 |
|----------------|-------|-----------|---------|---------------------------------------------|
| exportInterval | Int64 | no         | 300       | The duration between two exports in seconds |


### ComputeExportConfig
The features that should be exported by OpenDC

| Variable                 | Type         | Required? | Default      | Description                                                           |
|--------------------------|--------------|-----------|--------------|-----------------------------------------------------------------------|
| hostExportColumns        | List[String] | no        | All features | The features that should be exported to the host output file.         |
| taskExportColumns        | List[String] | no        | All features | The features that should be exported to the task output file.         |
| powerSourceExportColumns | List[String] | no        | All features | The features that should be exported to the power source output file. |
| serviceExportColumns     | List[String] | no        | All features | The features that should be exported to the service output file.      |


### Topology
Defines the topology on which the workload will be run.

:::info
For more information about the Topology go [here](Topology)
:::

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| pathToFile  | string | yes       | N/A     | Path to the JSON file defining the topology.                        |

### Workload
Defines the workload that needs to be executed.

:::info
For more information about workloads go [here](Workload)
:::

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| pathToFile  | string | yes       | N/A     | Path to the file containing the workload trace.                     |
| type        | string | yes       | N/A     | Type of the workload (e.g., "ComputeWorkload").                     |

### Allocation Policy
Defines the allocation policy that should be used to decide on which host each task should be executed

:::info Code
The different allocation policies that can be used can be found [here](https://github.com/atlarge-research/opendc/blob/master/opendc-compute/opendc-compute-simulator/src/main/kotlin/org/opendc/compute/simulator/scheduler/ComputeSchedulers.kt)
:::

| Variable   | Type   | Required? | Default | Description                |
|------------|--------|-----------|---------|----------------------------|
| policyType | string | yes       | N/A     | Type of allocation policy. |

### FailureModel
The failure model that should be used during the simulation
See [FailureModels](FailureModel) for detailed instructions.

### CheckpointModel
The checkpoint model that should be used to create snapshots.

| Variable                  | Type   | Required? | Default | Description                                                                                                         |
|---------------------------|--------|-----------|---------|---------------------------------------------------------------------------------------------------------------------|
| checkpointInterval        | Int64  | no         | 3600000 | The time between checkpoints in ms                                                                                  |
| checkpointDuration        | Int64  | no         | 300000  | The time to create a snapshot in ms                                                                                 |
| checkpointIntervalScaling | Double | no         | 1.0     | The scaling of the checkpointInterval after each succesful checkpoint. The default of 1.0 means no scaling happens. |


## Examples
In the following section, we discuss several examples of Scenario files. Any scenario file can be verified using the
JSON schema defined in [schema](TopologySchema).

### Simple

The simplest scneario that can be provided to OpenDC is shown below:
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
            "policyType": "Mem"
        }
    ]
}
```

This scenario creates a simulation from file topology1, located in the topologies folder, with a workload trace from the
bitbrains-small file, and an allocation policy of type Mem. The simulation is run once (by default), and the default
name is "".

### Complex
Following is an example of a more complex topology:
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
            "policyType": "Mem"
        },
        {
            "policyType": "Mem-Inv"
        }
    ]
}
```

This scenario runs a total of 12 experiments. We have 3 topologies (3 datacenter configurations), each simulated with
2 distinct workloads, each using a different allocation policy (either Mem or Mem-Inv).
