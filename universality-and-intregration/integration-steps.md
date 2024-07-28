# Integration Steps

We build our tool towards performance, scalability, and **universality**. In this document, we present the steps the
steps to integrate our tool into your simulation infrastructure.

## 1. Adapting the simulation output

The first step is to adapt the I/O of your simulation to the format of our tool. The folder structure should have the
following
setup:

```
[1] 🔧- simulation-folder-name
[2] 🔒| - inputs
[3] 🔧| | - multi-meta-configurator.json
[4] 🔧| | - {other input files / folders}
[5] 🔒| - outputs
[6] 🔒| | - raw-output
[7] 🔒| | | - 0
[8] 🔒| | | | - seed=0
[9] 🔧| | | | | - {simulation_data_file}.parquet
[10]🔧| | | | | - {any other files}
[11]🔒| | | - 1
[12]🔒| | | | - seed=0
[13]🔧| | | | | - host.parquet
[14]🔧| | | | | - {any other files}
[15]🔧| | | - metamodel
[16]🔧| | | | | - {your_metric_name}.parquet
[17]🔧| | - {any other files / folders}
```

- 🔧 = flexible, the name of the folder/file can differ.
- 🔒 = fixed, the name of the folder/file must be the same.
  <br><br>
- [1] = the name of the analyzed folder
- [2] = the inputs folder, containing various inputs / configuration files
- [3] = the name of the configuration file for the Multi-Meta-Model, can be (almost) any name, but needs to be a JSON
  file
- [4] = any other input files or folders
- [5] = output folder, containing the raw-output. can contain any other files or folders, besides the raw-output folder.
  After running a simulation, also a "simulation-analysis" folder will be generated
- [6] = raw-output folder, containing the raw output of the simulation
- [7],[11] = the IDs of the models. Must always start from zero. Possible values are 0, 1, 2 ... n, metamodel. The id
  of "metamodel" is reserved for the Meta-Model. Any simulation data in the respective folder will be treated as
  Meta-Model data.
- [8],[12] = the seed of the simulation
- [9],[13] = the file in which the simulation data is stored. The name of the file can differ, but it must be a parquet
  file.
- [10],[14] = any other files or folders
- [15] = the Meta-Model folder, optional. If the folder is present, the data in the folder will be treated as Meta-Model
  data.
- [16] = the Meta-Model output. The name of the file is of the type ```{your_metric_name}.parquet```. For example, if
  you analyze CO2 emissions, the file will be named ```co2_emissions.parquet```.
- [17] = any other files or folders

## Main.py Setup

#### Arguments

"experiments/experiment-2-window-performance-analysis/outputs/" "
experiments/experiment-2-window-performance-analysis/inputs/analyzer.json"
Main.py takes two arguments:

1. Argument 1 is the path to the output directory where the Multi-Meta-Model output files will be stored.
2. Argument 2 is the path to the input file that contains the configuration of the Multi-Meta-Model.

e.g.,

```json
"simulation-123/outputs/" "simulation-123/inputs/multi-meta-configurator.json"
```

#### Working directory

Make sure to set the working directory to the directory where the main.py file is located.

e.g.,

```
/your/path/to-analyzer/src/main/python
```

If you are using OpenDC, you can set the working directory to the following path:

```
/your/path/opendc/opendc-analyze/src/main/python
```
