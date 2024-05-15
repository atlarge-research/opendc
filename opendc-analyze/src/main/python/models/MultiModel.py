import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import pyarrow.parquet as pq

import utils
from .Model import Model


class MultiModel:
    def __init__(self, input_metric, window_size, aggregation_function="median"):
        # the following metrics are set in the latter functions
        self.measure_unit = None
        self.metric = None
        self.raw_models = []
        self.aggregated_models = []
        self.output_folder = None
        self.input_folder = utils.RAW_OUTPUT_FOLDER_PATH
        self.window_size = window_size
        self.aggregation_function = "median"

        # run init functions
        self.check_and_set_metric(input_metric)
        self.set_output_folder()
        self.init_models()

        # compute the multimodel on initialization
        self.computed_data = []
        self.compute_windowed_aggregation()

    """
    The set_output_folder function sets the output folder based on the metric chosen. If the metric is power_draw,
    the output folder is set to the energy analysis folder. If the metric is carbon_emission, the output folder is set
    to the emissions analysis folder.

    In this folder, there is a file "analysis.txt" which saves data from the simulation analysis.

    @return: None, but sets the self.output_folder attribute.
    """

    def set_output_folder(self):
        if self.metric == "power_draw":
            self.output_folder = utils.ENERGY_ANALYSIS_FOLDER_PATH
            # create a new file called analysis.txt
            with open(utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + utils.ENERGY_ANALYSIS_FOLDER_NAME + "/analysis.txt",
                      "a") as f:
                f.write("")
        elif self.metric == "carbon_emission":
            self.output_folder = utils.EMISSIONS_ANALYSIS_FOLDER_PATH
            with open(
                utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + utils.EMISSIONS_ANALYSIS_FOLDER_NAME + "/analysis.txt",
                "a") as f:
                f.write("")

        else:
            raise ValueError("Invalid metric. Please choose from 'power_draw', 'emissions'")

    """
    The init_models function takes the raw data from the simulation output and loads into the model attributes.
    Further, it aggregates the models that have topologies with 2 or more hosts.

    @return: None, but sets (initializes) the self.raw_models and self.aggregated_models attributes.
    """

    def init_models(self):
        folder_prefix = "./raw-output"

        for simulation_folder in os.listdir(folder_prefix):
            raw_model = Model(host=pd.read_parquet(f"{folder_prefix}/{simulation_folder}/seed=0/host.parquet"))

            # push simulation model raw, not aggregated
            self.raw_models.append(raw_model)

            # aggregate and push the model
            processed_raw_model = raw_model.host.select_dtypes(include=[np.number]).groupby("timestamp")
            processed_raw_model = processed_raw_model[self.metric].aggregate("sum")
            self.aggregated_models.append(processed_raw_model)

    """
    This function serves as an error prevention mechanism. It checks if the input metric is valid.
    If not, it raises a ValueError.
    @:return None, but sets the self.metric and self.measure_unit attributes. It can also raise an error.
    """

    def check_and_set_metric(self, input_metric):
        if input_metric not in ["power_draw", "carbon_emission"]:
            raise ValueError("Invalid metric. Please choose from 'power_draw', 'carbon_emission'")
        self.metric = input_metric
        self.measure_unit = "W" if self.metric == "power_draw" else "gCO2"

    """
    This function takes each model and aggregates values inside the chunks of the host data. The chunk size is taken
    from the window_size of the self. The aggregation function is taken as an argument.

    @:param aggregation_function: the function to aggregate the data, default is np.median(numeric_only=True)

    """

    def compute_windowed_aggregation(self):
        print("Computing windowed aggregation for " + self.metric)
        for model in self.aggregated_models:
            # Select only numeric data for aggregation
            numeric_values = model.values

            # Calculate the median for each window
            windowed_data = self.mean_of_chunks(numeric_values, self.window_size)
            self.computed_data.append(windowed_data)

    def generate(self):
        self.setup_plot()
        self.plot_windowed_aggregation()
        self.save_plot()

    def save_plot(self):
        folder_prefix = "./" + utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + self.metric + "/"
        plt.savefig(
            folder_prefix + "multimodel_metric=" + self.metric + "_window_size=" + str(self.window_size) + ".png")

    def setup_plot(self):
        plt.figure(figsize=(30, 10))
        plt.title(self.metric)
        plt.xlabel("Time [s]")
        plt.ylim(
            0,
            self.get_y_lim()
        )
        plt.ylabel(self.metric + " [W]")
        plt.grid()

    def plot_windowed_aggregation(self):
        i = 0
        for model in self.computed_data:
            plt.plot(model, label=i)
            i = i + 1

        plt.legend()

    def mean_of_chunks(self, np_array, chunk_size):
        return [np.mean(np_array[i:i + chunk_size]) for i in range(0, len(np_array), chunk_size)]

    def get_y_lim(self):
        return max([max(model) for model in self.computed_data]) * 1  # max from the computed_data bi-dim array + 10%
