import os
import time
from matplotlib import pyplot as plt
from input_parser import read_input
from models.MultiModel import MultiModel
from models.MetaModel import MetaModel
import pandas as pd
from copy import deepcopy  # Import deepcopy

from accuracy_evaluator import accuracy_evaluator

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

    metamodel_time_series = MetaModel(multimodel=multimodel_time_series)
    metamodel_time_series.output()

    metamodel_cumulative = MetaModel(multimodel=multimodel_cumulative)
    metamodel_cumulative.output()

    metamodel_cumulative_time_series = MetaModel(multimodel=multimodel_cumulative_time_series)
    metamodel_cumulative_time_series.output()




