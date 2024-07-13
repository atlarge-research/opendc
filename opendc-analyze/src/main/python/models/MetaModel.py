import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

import utils
from .Model import Model

"""
The MetaModel class is used to compute / aggregate a new model, by using the MultiModel class. The MetaModel takes
data from MultiModel and computes a new model, at a certain granularity, using a function (e.g., median, mean).

While mean succeeded in various sciences, we will further use median, as it is more robust to outliers. We will also
further analyze, integrate, and evaluate the MetaModel against individual models and test various methodologies of
aggregation (e.g., mathematical functions, machine learning models).
"""


class Metamodel:
    def __init__(self, multimodel):
        self.multimodel = multimodel
        self.models = multimodel.models
        self.metric = multimodel.metric
        self.measure_unit = multimodel.measure_unit
        self.window_size = multimodel.window_size
        self.metamodel_data = []
        self.compute()  # compute the metamodel on initialization

    """
    The compute function takes each model and computes the mean of the chunks of the host data,
    at a granularity defined by the window-size.

    This function is called on initialization and computes the MetaModel data.
    """

    def compute(self):
        for index in range(len(self.models[0].processed_host_data)):
            array_at_index = []
            for model in self.models:
                if index + self.window_size < len(model.processed_host_data):
                    array_at_index.append(model.processed_host_data[index])

            median_at_granularity = np.median(array_at_index)
            self.metamodel_data.append(median_at_granularity)

    """
    The generate function sets up the plot, plots the data and saves the plot.
    """

    def generate(self):
        self.setup_plot()
        self.plot()
        self.save_plot()

    """
    Set up the plot for the MetaModel.
    """

    def setup_plot(self):
        plt.figure(figsize=(30, 10))
        plt.title(self.metric)
        plt.xlabel("Time [s]")
        plt.ylabel(self.metric + self.measure_unit)
        plt.ylim(0, self.multimodel.set_y_axis_lim())

        # multiply all the values from the x-axis by the window size
        plt.xticks(
            np.arange(0, len(self.metamodel_data), step=self.window_size),
            np.arange(0, len(self.metamodel_data) * self.window_size, step=100 * self.window_size)
        )
        plt.grid()

    """
    Plot the processed MetaModel data.
    """

    def plot(self):
        plt.plot(self.metamodel_data)

    """
    Save the plot in the analysis folder.
    """

    def save_plot(self):
        folder_prefix = "./" + utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + self.metric + "/"
        plt.savefig(folder_prefix + "metamodel_metric=" + self.metric + "_window=" + str(self.window_size) + ".png")
