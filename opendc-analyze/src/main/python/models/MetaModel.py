import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

import utils
from .Model import Model

class Metamodel:
    def __init__(self, multimodel):
        self.multimodel = multimodel
        self.raw_models = multimodel.raw_models
        self.metric = multimodel.metric
        self.measure_unit = multimodel.measure_unit
        self.window_size = multimodel.window_size
        self.multimodel_data = multimodel.computed_data
        self.metamodel_data = []

    """
    The compute function takes each model and computes the mean of the chunks of the host data,
    at a granularity defined by the window-size.
    """
    def compute(self):
        for granularity in range(len(self.multimodel_data[0])):
            array_at_granularity = []
            for model in self.multimodel_data:
                array_at_granularity.append(model[granularity])

            median_at_granularity = np.median(array_at_granularity)
            self.metamodel_data.append(median_at_granularity)

    def generate(self):
        self.compute()
        self.setup_plot()
        self.plot()
        self.save_plot()

    def setup_plot(self):
        plt.figure(figsize=(30, 10))
        plt.title(self.metric)
        plt.xlabel("Time [s]")
        plt.ylabel(self.metric + self.measure_unit)
        plt.ylim(0, 400)
        plt.grid()

    def plot(self):
        plt.plot(self.metamodel_data)

    def save_plot(self):
        folder_prefix = "./" + utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + self.metric + "/"
        plt.savefig(folder_prefix + "metamodel_metric=" + self.metric + "_window_size=" + str(self.window_size) + ".png")
