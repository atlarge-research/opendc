import os

from models.MetaModel import MetaModel
from models.MultiModel import MultiModel

"""
Goal: analyze the accuracy, per model, using MAPE, NAD, RMSLE.

Experiment 5 computes a multi-model with various models, then a meta-model. It then feeds in the accuracy evaluator
and generates a peformance report, per model.
"""


def experiment_6(user_input, path):
    """
    Goal: plot the Multi-Model and the Meta-Model, with all types of plots.
    :param multimodel:
    :return: N/A, but output the files to the output folder.
    """

    multimodel_time_series = MultiModel(user_input=user_input, path=path)
    multimodel_time_series.plot_type = "time_series"
    multimodel_time_series.plot_title = "Time Series"
    multimodel_time_series.y_label = "Energy Usage [kW]"
    multimodel_time_series.unit_scaling = 3

    multimodel_cumulative = MultiModel(user_input=user_input, path=path)
    multimodel_cumulative.plot_type = "cumulative"
    multimodel_cumulative.plot_title = "Cumulative"

    multimodel_cumulative_time_series = MultiModel(user_input=user_input, path=path)
    multimodel_cumulative_time_series.plot_type = "cumulative_time_series"
    multimodel_cumulative_time_series.plot_title = ""

    input_folder_path = os.path.join(multimodel_time_series.output_folder_path, "../../")
    input_folder_path = input_folder_path.split('/')
    input_folder_path = '/'.join(input_folder_path[:-4])
    input_folder_path = input_folder_path + "/inputs/surf-sara/trace/ground_truth.parquet"

    MetaModel(multimodel=multimodel_time_series)
    MetaModel(multimodel=multimodel_cumulative)
    MetaModel(multimodel=multimodel_cumulative_time_series)



def experiment6_fr6(user_input, path):
    """
    Goal: plot the Meta-Model by itself (without other models), in a time series graph, with the window size of 1, 100, and 1000.
    :param user_input: dictionary of user input
    :param path: string of the path to the input file
    :return:
    """
    user_input["window_size"] = 10
    multimodel_window_10 = MultiModel(user_input=user_input, path=path)
    multimodel_window_10.plot_type = "time_series"
    multimodel_window_10.plot_title = "Window 10"
    MetaModel(multimodel=multimodel_window_10)

    user_input["window_size"] = 250
    multimodel_window_250 = MultiModel(user_input=user_input, path=path)
    multimodel_window_250.plot_type = "time_series"
    multimodel_window_250.plot_title = "Window 250"
    MetaModel(multimodel=multimodel_window_250)

    user_input["window_size"] = 5000
    multimodel_window_5000 = MultiModel(user_input=user_input, path=path)
    multimodel_window_5000.plot_type = "time_series"
    multimodel_window_5000.plot_title = "Window 5000"
    MetaModel(multimodel=multimodel_window_5000)
