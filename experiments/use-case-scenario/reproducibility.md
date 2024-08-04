# Experiment 8 - metamodel accurayc analysis reproducibility

It is important to run Part A before Part B. Part A runs 1 model, while Part B runs 8 models. The results are saved
in the same folder, then overwritten by the next run, but not deleted. Your don't want to run a single-model scenario
with 8 models :).

Make sure main.py looks like this:
```python
from experiments import usecase
# other imports

def main():
    usecase.usecase(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1]
    )
```

## Part A

#### Step 1

Ensure the sampling rate of 300 seconds. Run ScenarioCLI for energy usage:
```
--scenario-path "experiments/use-case-scenario/inputs/scenario-energy-usage-1-model.json" -p 4 -a
```

Working directory:
```
/your/path/to/opendc/
```

#### Step 2

Run ScenarioCLI for energy usage:
```
--scenario-path "experiments/use-case-scenario/inputs/scenario-co2-emissions-1-model.json" -p 4 -a
```

Keep the same working directory.

<br><br>
**!IMPORTANT**

**In between the parts, make sure to rename, or save separately the plots. New files may have the same name, and
overwrite old ones.**

<br><br>

## Part B

#### Step 3

Now, run ScenarioCLI for 8 models. Energy usage:

```
--scenario-path "experiments/use-case-scenario/inputs/scenario-energy-usage-8-models.json" -p 4 -a
```

Keep the same working directory.

#### Step 4

```
--scenario-path "experiments/use-case-scenario/inputs/scenario-co2-emissions-8-models.json" -p 4 -a
```

Keep the same working directory.

The results will be outputted, all, in the 
