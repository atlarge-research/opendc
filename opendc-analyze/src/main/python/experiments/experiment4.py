import os
import time
from matplotlib import pyplot as plt
from input_parser import read_input
from models.MultiModel import MultiModel

"""
Goal: plot all types of plots, with more than 4 models.

Experiment 4 plots all types of plots the Multi-Model supports, namely time-series, cumulative, and cumulative-time-series
plots, for more than 4 models.
"""


def experiment_4(argv1, argv2):
    user_input = read_input(argv2)

    multimodel = MultiModel(
        user_input,
        path=argv1
    )
    multimodel.generate_plot()
