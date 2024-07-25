import time

from models.MetaModel import MetaModel
from models.MultiModel import MultiModel

"""
Goal: analyze the accuracy, per model, using MAPE, NAD, RMSLE.

Experiment 5 computes a multi-model with various models, then a meta-model. It then feeds in the accuracy evaluator
and generates a peformance report, per model.
"""


def experiment_7(user_input, path):
    """
    GOAL: Analyze the time performance, for a Meta-Model computed on 2,016, 10,080, 20,160, 100,800, 201,600 samples, with
    metafunctions of 'mean', 'median', the window sizes of 1, 10, 100, 1000, and outputting (the models itself and plots
    time-series, cumulative, and cumulative-time-series).
    :param user_input:
    :param path:
    :return:
    """

    analysis_file_path = path + "/performance_report.txt"
    # samples_folders = ["samples=2,016", "samples=10,080", "samples=20,160", "samples=100,800", "samples=201,600"]
    samples_folders = ["samples=2,016", "samples=10,080"]
    # samples_folders = ["samples=201,600"]
    window_sizes = [1, 10, 100, 1000]
    plot_types = ['time_series', 'cumulative', 'cumulative_time_series']
    meta_functions = ['mean', 'median']

    ascii_art = f"""
    *******************************************
    * Running Experiment 2
    * Time: {time.strftime('%Y-%m-%d %H:%M:%S')}
    *******************************************
    """

    with (open(analysis_file_path, 'a')) as f:
        f.write(ascii_art)
        for samples_folder in samples_folders:
            f.write(f"\n\n===Sample folder: {samples_folder}===\n")
            for meta_function in meta_functions:
                for plot_type in plot_types:
                    for window_size in window_sizes:
                        argv2 = path + f"{samples_folder}"
                        starting_time = time.time()
                        multimodel = MultiModel(
                            user_input,
                            path=argv2,
                            window_size=window_size
                        )
                        multimodel.plot_type = plot_type
                        metamodel = MetaModel(multimodel=multimodel)
                        metamodel.meta_function = meta_function

                        metamodel.output()

                        ending_time = time.time()
                        elapsed_time = ending_time - starting_time
                        f.write(
                            f"Meta-Function={meta_function}, Plot-Type={plot_type}, Window-Size={window_size}: {round(elapsed_time, 3)} seconds\n")
                        print(
                            f"Meta-Function={meta_function}, Plot-Type={plot_type}, Window-Size={window_size}: {round(elapsed_time, 3)} seconds\n")
