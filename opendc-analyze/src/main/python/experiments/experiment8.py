import os
import pandas as pd

from accuracy_evaluator import accuracy_evaluator
from models.MultiModel import MultiModel

"""
Goal: analyze the accuracy, per model, using MAPE, NAD, RMSLE.

Experiment 5 computes a multi-model with various models, then a meta-model. It then feeds in the accuracy evaluator
and generates a peformance report, per model.
"""


def experiment_8(user_input, path):
    """
    GOAL: Analyze the accuracy of each individual model, and output in a file.
    Then, analyze the accuracy of Meta-Model using mean, median, and the meta-equation.
    Analyze against ground truth, located in inputs/surf-sara/trace/ground_truth.parquet.
    """
    user_input["meta_function"] = "mean"
    multimodel_mean_meta = MultiModel(
        user_input=user_input,
        path=path
    )

    input_folder_path = os.path.join(multimodel_mean_meta.output_folder_path, "../../")
    input_folder_path = input_folder_path.split('/')
    input_folder_path = '/'.join(input_folder_path[:-4])
    input_folder_path = input_folder_path + "/inputs/surf-sara/trace/ground_truth.parquet"

    ground_truth = pd.read_parquet(input_folder_path)
    ground_truth = ground_truth.groupby('timestamp')['surfsara_power_usage'].sum().values

    accuracy_evaluator(
        real_data=ground_truth,
        multi_model=multimodel_mean_meta,
        compute_mape=True,
        compute_nad=True,
        compute_rmsle=True,
        rmsle_hyperparameter=0.5
    )

    user_input["meta_function"] = "median"
    multimodel_median_meta = MultiModel(
        user_input=user_input,
        path=path
    )

    accuracy_evaluator(
        real_data=ground_truth,
        multi_model=multimodel_median_meta,
        compute_mape=True,
        compute_nad=True,
        compute_rmsle=True,
        rmsle_hyperparameter=0.5
    )
