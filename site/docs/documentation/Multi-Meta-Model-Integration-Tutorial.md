# Integration Steps of the Multi-Meta-Model

We build our tool towards performance, scalability, and **universality**. In this document, we present the steps to
integrate our tool into your simulation infrastructure.

If you are using OpenDC, none of adaptation steps are necessary, yet they can be useful to understand the structure
of the tool. Step 3 is still necessary.

## Step 1: Adapt the simulator output folder structure

The first step is to adapt the I/O of your simulation to the format of our tool. The output folder structure should have
the
following format:

```
[1] ── {simulation-folder-name} 📁 🔧  
[2]    ├── inputs 📁 🔒  
[3]    │   └── {multi-meta-model-config-file}.json 📄 🔧  
[4]    │   └── {other input files / folders} 🔧  
[5]    ├── outputs 📁 🔒   
[6]    │   ├── raw-output 📁 🔒  
[7]    │   │   ├── 0 📁 🔒  
[8]    │   │   │   └── seed={your_seed}🔒  
[9]    │   │   │       └── {simulation_data_file}.parquet 📄 🔧  
[10]   │   │   │       └── {any other files / folders} ⚪  
[11]   │   │   ├── 1 📁 ⚪ 🔒
[12]   │   │   │   └── seed={your_seed} 📁 ⚪ 🔒
[13]   │   │   │       └── {simulation_data_file}.parquet 📄 ⚪ 🔧
[14]   │   │   │       └── {any other files / folders} ⚪󠁪  
[15]   │   │   ├── metamodel 📁 ⚪  
[16]   │   │      └── seed={your_seed} 📁 ⚪  
[17]   │   │           └── {your_metric_name}.parquet 📄 ⚪  
[18]   │   │           └── {any other files / folders} ⚪  
[19]   │   └── {any other files / folders} 📁 ⚪  
[20]|  └──{any other files / folders} 📁 ⚪  
```

📄 = file <br>
📁 = folder <br>
🔒 = fixed, the name of the folder/file must be the same.<br>
🔧 = flexible, the name of the folder/file can differ. However, the item must be present.<br>
⚪ = optional and flexible. The item can be absent. <br>

- [1] = the name of the analyzed folder.
- [2] = the _inputs_ folder, containing various inputs / configuration files.
- [3] = the configuration file for the Multi-Meta-Model, flexible naming, but needs to be a JSON file
- [4],[10],[14],[18],[19],[20] = any other input files or folders.
- [5] = the _outputs_ folder, containing the raw-output. can contain any other files or folders, besides the raw-output
  folder.
  After running a simulation, also a "simulation-analysis" folder will be generated in this folder.
- [6] = raw-output folder, containing the raw output of the simulation.
- [7],[11] = the IDs of the models. Must always start from zero. Possible values are 0, 1, 2 ... n, and "metamodel". The
  id
  of "metamodel" is reserved for the Meta-Model. Any simulation data in the respective folder will be treated as
  Meta-Model data.
- [8],[12] = the seed of the simulation. the seed must be the same for both [8], [12], and other equivalent, further
  files.
- [9],[13] = the file in which the simulation data is stored. The name of the file can differ, but it must be a parquet
  file.
- [15] = the Meta-Model folder, optional. If the folder is present, its data will be treated as Meta-Model data.
- [16] = the Meta-Model seed folder. The seed must be the same as the seed of the simulation.
- [17] = the Meta-Model output. The name of the file is of the type ```{your_metric_name}.parquet```. For example, if
  you analyze CO2 emissions, the file will be named ```co2_emissions.parquet```.

---

## Step 2: Adapt the simulation file format

The simulator data file must be a 🪵 _parquet_ 🪵 file.

The file must contain (at least) the columns:

- timestamp: the timestamp, in miliseconds, of the data point (e.g., 30000, 60000, 90000) - the time unit is flexible.
- {metric_name}: the value of the metric at the given timestamp. This is the metric analyzed (e.g., CO2_emissions,
  energy_usage).

e.g., if you are analyzing the CO2 emissions of a datacenter, for a timeperiod of 5 minutes, and the data is sampled
every 30 seconds, the file will look like this:

| timestamp | co2_emissions |
|-----------|---------------|
| 30000     | 31.2          |
| 60000     | 31.4          |
| 90000     | 28.5          |
| 120000    | 31.8          |
| 150000    | 51.5          |
| 180000    | 51.2          |
| 210000    | 51.4          |
| 240000    | 21.5          |
| 270000    | 21.8          |
| 300000    | 21.2          |

---

## Step 3: Running the Multi-Meta-Model

### 3.1 Setup the Simulator Specifics

Update the simulation folder name ([9], [13], [17] from Step 1), in the
file [simulator_specifics.py](opendc/src/python/simulator_specifics.py).

### 3.2 Setup the python program arguments

```python

### Arguments for Main.py Setup

[//]: # ("experiments/experiment-2-window-performance-analysis/outputs/" ")

[//]: # (experiments/experiment-2-window-performance-analysis/inputs/analyzer.json")
Main.py takes two arguments:

1. Argument 1 is the path to the output directory where the Multi-Meta-Model output files will be stored.
2. Argument 2 is the path to the input file that contains the configuration of the Multi-Meta-Model.

e.g.,

```json
"simulation-123/outputs/" "simulation-123/inputs/multi-meta-configurator.json"
```

### 3.3 Working directory Main.py Setup

Make sure to set the working directory to the directory where the main.py file is located.

e.g.,

```
/your/path/to-analyzer/src/main/python
```

If you are using OpenDC, you can set the working directory to the following path:

```
/your/path/opendc/opendc-analyze/src/main/python
```

---

## Optional: Step 4: Simulate and analyze, with one click

The simulation and analysis can be executed as a single command; if no errors are encountered, from the user
perspective,
this operation is atomic. We integrated the Multi-Meta-Model into OpenDC to facilitate this process.

To further integrate the Multi-Meta-Model into any simulation infrastructure, the Multi-Meta-Model needs to called from
the simulation infrastructure, and provided the following running setup:

1. script language: Python
2. argument 1: the path of the output directory, in which the Multi-Meta-Model output files will be stored
3. argument 2: the path of the input file, containing the configuration of the Multi-Meta-Model
4. other language-specific setup

For example, the integration of the Multi-Meta-Model into OpenDC can be found
in [Analyzr.kt](opendc-analyze/src/main/kotlin/Analyzr.kt).
Below, we provide a snippet of the code:

```kotlin
val ANALYSIS_SCRIPTS_DIRECTORY: String = "./opendc-analyze/src/main/python"
val ABSOLUTE_SCRIPT_PATH: String =
    Path("$ANALYSIS_SCRIPTS_DIRECTORY/main.py").toAbsolutePath().normalize().toString()
val SCRIPT_LANGUAGE: String = "python3"

fun analyzeResults(outputFolderPath: String, analyzerSetupPath: String) {
    val process = ProcessBuilder(
        SCRIPT_LANGUAGE,
        ABSOLUTE_SCRIPT_PATH,
        outputFolderPath, // argument 1
        analyzerSetupPath // argument 2
    )
        .directory(Path(ANALYSIS_SCRIPTS_DIRECTORY).toFile())
        .start()

    val exitCode = process.waitFor()
    if (exitCode == 0) {
        println("[Analyzr.kt says] Analysis completed successfully.")
    } else {
        val errors = process.errorStream.bufferedReader().readText()
        println("[Analyzr.kt says] Exit code ${exitCode}; Error(s): $errors")
    }
}
```
