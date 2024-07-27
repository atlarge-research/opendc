# Experiment 2 - reproducibility

**Option 1:** Run Scenario CLI, with the following setup.

Main.py code:
```python
from experiments import experiment2
# other imports

def main():
    experiment2.experiment_2()
```

ScenarioCLI arguments:
```
--scenario-path "experiments/experiment-2-window-performance-analysis/inputs/scenario.json" -p 4 -a
```

ScenarioCLI working directory:
```
/Users/raz/atlarge/opendc/
```


**Option 2:** If already have the simulation output files, run only the main.py and skip the simulation time.

Main.py code:
```python
from experiments import experiment2
# other imports

def main():
    experiment2.experiment_2()
```

Main.py arguments

```
"experiments/experiment-2-window-performance-analysis/outputs/" "experiments/experiment-2-window-performance-analysis/inputs/analyzer.json"
```

The output of the experiment is generated in 
```opendc-analyze/src/main/python/analysis.txt```.
