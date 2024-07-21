import os
import time
from matplotlib import pyplot as plt
from input_parser import read_input
from models.MultiModel import MultiModel
from accuracy_evaluator import accuracy_evaluator

"""
Goal: analyze the accuracy, per model, using MAPE, NAD, RMSLE.

Experiment 5 computes a multi-model with various models, then a meta-model. It then feeds in the accuracy evaluator
and generates a peformance report, per model.
"""


def experiment_5(argv1, argv2):
    user_input = read_input(argv2)

    multimodel = MultiModel(
        user_input,
        path=argv1
    )
    multimodel.generate_plot()
