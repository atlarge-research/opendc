import time

import utils
from models.MultiModel import MultiModel

"""
This experiment is used to compare the time taken to generate the analysis for different window sizes, for two metrics.
This provides insights in how much time is required to generate analysis.

The window sizes are [1, 10, 100, 1000, 10000]
The metrics are ["power_draw", "carbon_emission"]
We obtained for power_draw:
[w-size] - [time taken]
  1      - 0.825 seconds
  10     - 0.482 seconds
  100    - 0.412 seconds
  1000   - 0.397 seconds
  10000  - 0.379 seconds

@:param window_sizes: list of window sizes to compare, default is [1, 10, 100, 1000, 10000], increasing order of magnitude
@:param metrics: list of metrics to compare, default is ["power_draw", "carbon_emission"]
"""


def experiment_0(window_sizes=[1, 10, 100, 1000]):
    metric = "power_draw"
    analysis_file_path = utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + metric + "/" + "window_size_time_analysis-small.txt"
    with open(analysis_file_path, "a") as f:
        multimodel = MultiModel(metric, 1)
        number_of_samples = len(multimodel.models[0].raw_sim_data)
        f.write("\n\nWe are running window_size_time_analysis.experiment_small() for power_draw\n")
        f.write("Sample count in raw host data: " + str(number_of_samples) + "\n")

    for window_size in window_sizes:
        start = time.time()
        multimodel = MultiModel(metric, window_size)
        multimodel.generate_plot()
        with open(analysis_file_path, "a") as f:
            f.write(f"Time taken for window size {window_size}: {round(time.time() - start, 3)} seconds\n")

