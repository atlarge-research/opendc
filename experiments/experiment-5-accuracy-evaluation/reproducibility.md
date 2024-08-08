# Experiment 5 - accuracy evaluation

In this experiment, we are building a tool to evaluate the accuracy of the simulator, overall, and the 
accuracy of the meta-model tool.

#### Step 0

Ensure that the data from m3saSetup.json is not scaled. In other words, ensure ```"unit_scaling_magnitude": 1```.

#### Step 1
Run Scenario CLI, with the following setup.

ScenarioCLI arguments:
```
--scenario-path "experiments/experiment-5-accuracy-evaluation/inputs/scenario.json" -p 4 -a
```

ScenarioCLI working directory:
```
/your/path/opendc/
```

Make sure to have the sampling rate of the simulator set to 30. (Can be adjusted from ```ExportModelSpec.kt```)

#### Step 2
Now, if the simulation was successful, we should have the results in 
```
your/path/experiment-5-accuracy-evaluation/outputs/raw-output
```

We analyze the results in the Jupyter notebook 'output_analyzer.ipynb', located in the ```outputs``` directory.

#### Step 3
If running from main.py, main.py should contain
```python
def main():
    experiment5.experiment_5(
        MultiModel(
            user_input=read_input(sys.argv[2]),
            path=sys.argv[1],
        )
    )
```

Run main.py with the following arguments:
```
"experiments/experiment-5-accuracy-evaluation/outputs/" "experiments/experiment-5-accuracy-evaluation/inputs/m3saSetup.json"
```

#### Step 4
Analysis can be done using accuracy_evaluator, located in the same directory as main.py.
