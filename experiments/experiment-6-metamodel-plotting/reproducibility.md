# Experiment 6

#### Step 1
Run Scenario CLI, with the following setup.

ScenarioCLI arguments:
```
--scenario-path "experiments/experiment-6-metamodel-plotting/inputs/scenario.json" -p 4 -a
```

ScenarioCLI working directory:
```
/your/path/opendc/
```

#### Step 2
Now, if the simulation was successful, we should have the results in 
```
your/path/experiment-6-metamodel-plotting/outputs/raw-output
```

We analyze the results in the Jupyter notebook 'output_analyzer.ipynb', located in the ```outputs``` directory.

#### Step 3
If running from main.py, main.py should contain
```python
def main():
    experiment6.experiment_6(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )
```


#### Utils

- analyzer.json file, for FR2
```json
{
    "multimodel": true,
    "metamodel": true,
    "metric": "power_draw",
    "window_size": 100,
    "window_function": "mean",
    "meta_function": "median",
    "samples_per_minute": 1,
    "plot_type": "time_series",
    "x_label": "Sample count",
    "y_label": "Energy Usage [MW]",
    "plot_title": "Time Series Plot",
    "x_ticks_count": 5,
    "y_ticks_count": 5,
    "current_unit": "W",
    "unit_scaling_magnitude": 6
}
```


- analyzer.json file, for FR3 - CO2 emissions
```json
{
    "multimodel": true,
    "metamodel": true,
    "metric": "carbon_emission",
    "window_size": 10,
    "window_function": "mean",
    "meta_function": "median",
    "samples_per_minute": 1,
    "plot_type": "time_series",
    "x_label": "Sample count",
    "y_label": "Carbon Intensity [kgCO2]",
    "plot_title": "Time Series Plot",
    "x_ticks_count": 5,
    "y_ticks_count": 5,
    "current_unit": "gCO2",
    "unit_scaling_magnitude": 3
}

```


- analyzer.json file, for FR3 - Energy Usage
```json
{
    "multimodel": true,
    "metamodel": true,
    "metric": "power_draw",
    "window_size": 10,
    "window_function": "mean",
    "meta_function": "median",
    "samples_per_minute": 1,
    "plot_type": "cumulative_time_series",
    "x_label": "Sample count",
    "y_label": "Energy Usage [MW]",
    "plot_title": "Cumulative Time Series Plot",
    "x_ticks_count": 5,
    "y_ticks_count": 5,
    "current_unit": "W",
    "unit_scaling_magnitude": 6
}
```
