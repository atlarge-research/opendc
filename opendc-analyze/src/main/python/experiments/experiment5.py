import os
import time
from matplotlib import pyplot as plt
from input_parser import read_input
from models.MultiModel import MultiModel
import pandas as pd

from accuracy_evaluator import accuracy_evaluator

"""
Goal: analyze the accuracy, per model, using MAPE, NAD, RMSLE.

Experiment 5 computes a multi-model with various models, then a meta-model. It then feeds in the accuracy evaluator
and generates a peformance report, per model.
"""


def experiment_5(multimodel):
    input_folder_path = os.path.join(multimodel.output_folder_path, "../../")
    input_folder_path = input_folder_path.split('/')
    input_folder_path = '/'.join(input_folder_path[:-4])
    input_folder_path = input_folder_path + "/inputs/surf-sara/trace/ground_truth.parquet"

    ground_truth = pd.read_parquet(input_folder_path)
    ground_truth = ground_truth.groupby('timestamp')['surfsara_power_usage'].sum().values
    accuracy_evaluator(
        real_data=ground_truth,
        multi_model=multimodel,
        compute_mape=True,
        compute_nad=True,
        compute_rmsle=True,
        rmsle_hyperparameter=0.5,
    )



