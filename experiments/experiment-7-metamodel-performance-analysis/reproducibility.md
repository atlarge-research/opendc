# Experiment 7 - performance analysis

**Option 1:** Run Scenario CLI, with the following setup.

Main.py code:

```python
from experiments import experiment7
# other imports

def main():
    experiment7.experiment_7(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )
```

ScenarioCLI arguments:

```
--scenario-path "experiments/experiment-7-metamodel-performance-analysis/inputs/scenario.json" -p 4 -a
```

ScenarioCLI working directory:

```
/Users/raz/atlarge/opendc/
```

**Option 2:** If already have the simulation output files, run only the main.py and skip the simulation time.

Main.py code:

```python
from experiments import experiment7
# other imports

def main():
    experiment7.experiment_7(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )
```

Main.py arguments:

```
"experiments/experiment-7-metamodel-performance-analysis/outputs/" "experiments/experiment-7-metamodel-performance-analysis/inputs/m3saSetup.json"
```


