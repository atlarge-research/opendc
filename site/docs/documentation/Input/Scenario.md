The scenario of a simulation is defined using a JSON file. A scenario consists of one or more topologies, one or more
workloads, one or more allocation policies, a name and a number of times the simulation is being run.

## Schema

The schema for the scenario file is provided in [schema](ScenarioSchema)
In the following section, we describe the different components of the schema.

### General Structure

| Variable           | Type                                         | Required? | Default  | Description                                                       |
|--------------------|----------------------------------------------|-----------|----------|-------------------------------------------------------------------|
| name               | string                                       | no        | ""       | Name of the scenario, used for identification and referencing.    |
| topologies         | List[[Topology](#topology)]                  | yes       | N/A      | List of topologies used in the scenario.                          |
| workloads          | List[[Workload](#workload)]                  | yes       | N/A      | List of workloads to be executed within the scenario.             |
| allocationPolicies | List[[AllocationPolicy](#allocation-policy)] | yes       | N/A      | Allocation policies used for resource management in the scenario. |
| failureModels      | List[[FailureModel](#failuremodel)]          | no        | empty    | List of failure models to simulate various types of failures.     |
| exportModels       | List[[ExportModel](#exportmodel)]            | no        | empty    | Specifications for exporting data from the simulation.            |
| carbonTracePaths   | List[string]                                 | no        | null     | Paths to carbon footprint trace files.                            |
| analyzerPath       | string                                       | no        | null     | Path to the cofigurator file of M3SA.                             | 
| outputFolder       | string                                       | no        | "output" | Directory where the simulation outputs will be stored.            |
| initialSeed        | integer                                      | no        | 0        | Seed used for random number generation to ensure reproducibility. |
| runs               | integer                                      | no        | 1        | Number of times the scenario should be run.                       |

### Topology

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| pathToFile  | string | yes       | N/A     | Path to the JSON file defining the topology.                        |

### Workload

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| pathToFile  | string | yes       | N/A     | Path to the file containing the workload trace.                     |
| type        | string | yes       | N/A     | Type of the workload (e.g., "ComputeWorkload").                     |

### Allocation Policy

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| policyType  | string | yes       | N/A     | Type of allocation policy (e.g., "BestFit", "FirstFit").            |

### FailureModel

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| modelType   | string | yes       | N/A     | Type of failure model to simulate specific operational failures.    |

### ExportModel

| Variable    | Type   | Required? | Default | Description                                                         |
|-------------|--------|-----------|---------|---------------------------------------------------------------------|
| exportType  | string | yes       | N/A     | Specifies the type of data export model for simulation results.     |


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
            "pathToFile": "traces/bitbrains-small",
            "type": "ComputeWorkload"
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
    ],
    "analyzerPath": "experiments/experiment-2-window-performance-analysis/inputs/analyzer.json",
    "outputFolder": "experiments/experiment-2-window-performance-analysis/outputs/",
    "runs": 1
}
```

This scenario runs a total of 12 experiments. We have 3 topologies (3 datacenter configurations), each simulated with
2 distinct workloads, each using a different allocation policy (either Mem or Mem-Inv). The simulation is run once and
is outputted in the folder "experiments/experiment-2-window-performance-analysis/outputs/". The configurator for M3SA is
located in "experiments/experiment-2-window-performance-analysis/inputs/analyzer.json".
```
