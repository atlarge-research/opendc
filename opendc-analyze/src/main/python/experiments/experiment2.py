import time

from input_parser import read_input
from models.MultiModel import MultiModel

"""
Experiment 2 analyses the time required to compute a multimodel, using different window sizes, for different sizes of datasets.
The datasets have been generated using the sample rate of 3, 30, 60 and 300, which means that the infrastructure is samples
every 3, 30, 60 and 300 seconds respectively.

We save the data results in analysis.txt file, in the directory corresponding to experiment 2.
"""
def experiment_2():
    analysis_file_path = 'time_series.txt'
    sample_rates = [300, 60, 30, 6, 3]
    window_sizes = [1000, 100, 10, 1]
    argv1 = "experiments/experiment-2-window-performance-analysis/inputs/m3saSetup.json"

    ascii_art = f"""
    *******************************************
    * Running Experiment 2
    * Time: {time.strftime('%Y-%m-%d %H:%M:%S')}
    *******************************************
    """

    with open(analysis_file_path, 'a') as f:
        f.write(ascii_art)

        for sample_rate in sample_rates:
            f.write(f"\n\n===Sample rate: {sample_rate}===\n")
            for window_size in window_sizes:
                starting_time = time.time()

                argv2 = f"experiments/experiment-2-window-performance-analysis/outputs/sample-rate={sample_rate}s"

                multimodel = MultiModel(
                    read_input(argv1),
                    path=argv2,
                    window_size=window_size,
                )
                multimodel.generate_plot()

                ending_time = time.time()
                elapsed_time = ending_time - starting_time
                f.write(f"Time taken for window size {window_size}: {round(elapsed_time, 3)} seconds\n")

