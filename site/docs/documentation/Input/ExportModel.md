During simulation, OpenDC exports data to files (see [Output](/docs/documentation/Output.md)).
The user can define what and how data is exported using the `exportModels` parameter in the experiment file.

## ExportModel



| Variable            | Type                                    | Required? | Default   | Description                                                                                                                                                   |
|---------------------|-----------------------------------------|-----------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| exportInterval      | Int64                                   | no        | 300       | The duration between two exports in seconds                                                                                                                   |
| filesToExport       | Int64                                   | no        | 24        | How often OpenDC prints an update during simulation.                                                                                                          |  |
| computeExportConfig | [ComputeExportConfig](#checkpointmodel) | no        | Default   | The features that should be exported during the simulation                                                                                                    |
| filesToExport       | List[string]                            | no        | all files | List of the files that should be exported during simulation. The elements should be picked from the set ("host", "task", "powerSource", "battery", "service") |



### ComputeExportConfig
The ComputeExportConfig defines which features should be exported during the simulation.
Several features will always be exported, regardless of the configuration.
When not provided, all features are exported.


| Variable                 | Type         | Required? | Base                                                                   | Default      | Description                                                           |
|--------------------------|--------------|-----------|------------------------------------------------------------------------|--------------|-----------------------------------------------------------------------|
| hostExportColumns        | List[String] | no        | name <br/> cluster_name <br/> timestamp <br/> timestamp_absolute <br/> | All features | The features that should be exported to the host output file.         |
| taskExportColumns        | List[String] | no        | task_id <br/> task_name <br/> timestamp <br/> timestamp_absolute <br/> | All features | The features that should be exported to the task output file.         |
| powerSourceExportColumns | List[String] | no        | name <br/> cluster_name <br/> timestamp <br/> timestamp_absolute <br/> | All features | The features that should be exported to the power source output file. |
| batteryExportColumns     | List[String] | no        | name <br/> cluster_name <br/> timestamp <br/> timestamp_absolute <br/> | All features | The features that should be exported to the battery output file.      |
| serviceExportColumns     | List[String] | no        | timestamp <br/> timestamp_absolute <br/>                               | All features | The features that should be exported to the service output file.      |

### Example

```json
{
    "exportInterval": 3600,
    "printFrequency": 168,
    "filesToExport": ["host", "task", "service"],
    "computeExportConfig": {
        "hostExportColumns": ["power_draw", "energy_usage", "cpu_usage", "cpu_utilization"],
        "taskExportColumns": ["submission_time", "schedule_time", "finish_time", "task_state"],
        "serviceExportColumns": ["tasks_total", "tasks_pending", "tasks_active", "tasks_completed", "tasks_terminated", "hosts_up"]
    }
}
```
In this example: 
- the simulation will export data every hour (3600 seconds).
- The simulation will print an update every 168 seconds.
- Only the host, task and service files will be exported.
- Only a selection of features are exported for each file.

