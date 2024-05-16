import utils
import time

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
def exp1_window_sizes(window_sizes=[10, 100, 1000, 10000], metrics=["power_draw", "carbon_emission"]):
    for metric in metrics:
        utils.clean_analysis_file(metric)
        analysis_file_path = utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/"
        if metric == "power_draw":
            analysis_file_path += utils.ENERGY_ANALYSIS_FOLDER_NAME
        else:
            analysis_file_path += utils.EMISSIONS_ANALYSIS_FOLDER_NAME
        analysis_file_path += "/analysis.txt"
        with open(analysis_file_path, "a") as f:
            f.write("\n\n=====================\n| Exp1_window_sizes |\n=====================\n")

        for window_size in window_sizes:
            start = time.time()
            multimodel = MultiModel(metric, window_size)
            multimodel.generate_plot()
            with open(analysis_file_path, "a") as f:
                f.write(f"Time taken for window size {window_size}: {round(time.time() - start, 3)} seconds\n")
