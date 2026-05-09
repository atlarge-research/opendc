Each cluster has a power source that provides power to the hosts in the cluster.
A user can connect a power source to a carbon trace to determine the carbon emissions during a workload.

The power source consist of the following components:

| variable        | type         | Unit | required? | default        | description                                                                       |
|-----------------|--------------|------|-----------|----------------|-----------------------------------------------------------------------------------|
| name            | string       | N/A  | no        | PowerSource    | The name of the cluster. This is only important for debugging and post-processing |
| maxPower        | integer      | Watt | no        | Long.Max_Value | The total power that the power source can provide in Watt.                        |
| carbonTracePath | path/to/file | N/A  | no        | null           | A list of the hosts in a cluster.                                                 |

## Example

```json
{
  "carbonTracePath": "carbon_traces/AT_2021-2024.parquet"
}
```

This example creates a power source with infinite power draw that uses the carbon trace from the file `carbon_traces/AT_2021-2024.parquet`.
