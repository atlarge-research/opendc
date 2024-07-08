import math
import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import pyarrow.parquet as pq
import time

import utils
from .Model import Model

"""
A MultiModel is a collection of models. It is used to aggregate data from multiple instances of the Model class, and
further analyze it. The MultiModel takes the raw data from the simulation output and loads it into the model attributes.

The MultiModel uses a "windowed aggregation" technique to aggregate the data using a window size and a function. This
technique is similar to a convolution / moving average, which takes chunks of data and aggregates (e.g., average).

:param input_metric: the metric to analyze, either "power_draw" or "carbon_emission"
:param window_size: the size of the window to aggregate the data (e.g., an array of 1000 elements, windowed with window_size=10,
                    would result in 100 elements)
:param aggregation_function: the function to aggregate the data, default is "median"
"""


class MultiModel:
    def __init__(self,user_input,path):
        self.user_input = user_input

        # the following metrics are set in the latter functions
        self.measure_unit = None
        self.metric = None
        self.path = path
        self.models = []

        self.folder_path = None
        self.output_folder_path = None
        self.raw_output_path = None
        self.analysis_file_path = None
        self.window_size = -1
        self.aggregation_function = "median"
        self.workload_time = 0
        self.max_model_len = 0

        self.x_label = None
        self.y_label = None
        self.y_min = None
        self.y_max = None

        # run init functions
        self.parse_user_input()
        self.check_and_set_metric(self.metric)
        self.set_paths()
        self.init_models()

        self.compute_windowed_aggregation()


    """
    This function is used to parse the user input. It takes the inputs from the user and sets the attributed of the
    Multi-Model.
    """
    def parse_user_input(self):
        self.window_size = self.user_input["window_size"]
        self.metric = self.user_input["metric"]
        self.aggregation_function = self.user_input["aggregation_function"]
        self.x_label = self.user_input["x_label"]
        self.y_label = self.user_input["y_label"]
        self.y_min = self.user_input["y_min"]
        self.y_max = self.user_input["y_max"]


    """
    This function serves as an error prevention mechanism. It checks if the input metric is valid.
    If not, it raises a ValueError.
    @:return None, but sets the self.metric and self.measure_unit attributes. It can also raise an error.
    """

    def check_and_set_metric(self, input_metric):
        if input_metric not in ["power_draw", "carbon_emission"]:
            raise ValueError("Invalid metric. Please choose from 'power_draw', 'carbon_emission'")
        self.metric = input_metric
        self.measure_unit = "[W]" if self.metric == "power_draw" else "[gCO2]"

    """
    The set_output_folder function sets the output folder based on the metric chosen. If the metric is power_draw,
    the output folder is set to the energy analysis folder. If the metric is carbon_emission, the output folder is set
    to the emissions analysis folder.

    In this folder, there is a file "analysis.txt" which saves data from the simulation analysis.

    @return: None, but sets the self.output_folder attribute.
    """

    def set_paths(self):
        # the output folder is the current path + self.path
        self.output_folder_path = os.getcwd() + "/" + self.path
        self.raw_output_path = os.getcwd() + "/" + self.path + "/raw-output"
        self.analysis_file_path = os.getcwd() + "/" + self.path + "/simulation-analysis/"
        os.makedirs(self.analysis_file_path, exist_ok=True)
        self.analysis_file_path = os.path.join(self.analysis_file_path, "analysis.txt")
        if not os.path.exists(self.analysis_file_path):
            with open(self.analysis_file_path, "w") as f:
                f.write("Analysis file created.\n")



    """
    The init_models function takes the raw data from the simulation output and loads into the model attributes.
    Further, it aggregates the models that have topologies with 2 or more hosts.

    @return: None, but sets (initializes) the self.raw_models and self.aggregated_models attributes.
    """

    def init_models(self):
        model_id = 0

        for simulation_folder in os.listdir(self.raw_output_path):
            path_of_parquet_file = f"{self.raw_output_path}/{simulation_folder}/seed=0/host.parquet"
            parquet_file = pq.read_table(path_of_parquet_file).to_pandas()
            raw_data = parquet_file.select_dtypes(include=[np.number]).groupby("timestamp")
            raw_data = raw_data[self.metric].aggregate("sum")

            if self.user_input["samples_per_minute"] > 0:
                total_values = len(raw_data)
                total_time = total_values * self.user_input["samples_per_minute"] / 60 / 24
                print("There are " + str(total_values) + " values in the raw data, hence the data is measured for a time of"
                                                         " " + str(total_time) + " days.")

            model = Model(
                raw_host_data=raw_data,
                id=model_id,
                path=self.output_folder_path
            )

            self.models.append(model)
            model_id += 1

        self.max_model_len = max([len(model.raw_host_data) for model in self.models])
        self.workload_time = math.floor(
            300 * self.max_model_len / 3600 / 24)  # a sample is taken every 300 seconds and converted to days

    """
    The MultiModel uses a "windowed aggregation" technique to aggregate the data using a window size and a function. This
    technique is similar to a convolution / moving average, which takes chunks of data and aggregates (e.g., average).
    The size of the window to aggregate the data (e.g., an array of 1000 elements, windowed with window_size=10, would
    result in 100 elements)
    """

    def compute_windowed_aggregation(self):
        print("Computing windowed aggregation for " + self.metric)
        for model in self.models:
            numeric_values = model.raw_host_data  # Select only numeric data for aggregation

            # Calculate the median for each window
            model.processed_host_data, model.margins_of_error = self.mean_of_chunks_and_margin_error(numeric_values,
                                                                                                     self.window_size)

    """
    Generates plot for the MultiModel from the already computed data. The plot is saved in the analysis folder.
    """

    def generate_plot(self):
        self.setup_plot()
        self.plot_processed_models()
        self.save_plot()

    """
    Set up the plot for the MultiModel.
    """

    def setup_plot(self):
        plt.figure(figsize=(10, 10))
        plt.title(self.metric)

        # position the xlabel in the right of the graph
        plt.xlabel(self.x_label)
        plt.ylim(self.get_y_lim())
        plt.ylabel(self.metric + " " + self.measure_unit)
        # Add grid for better visibility

        plt.grid()

    """
    Plot the processed models, after the windowed aggregation is computed.
    """

    def plot_processed_models(self):
        for model in self.models:
            plt.plot(model.processed_host_data, label=("Model " + str(model.id) + "-" + model.experiment_name))

        plt.legend()

    """
    Save the plot in the analysis folder.
    """

    def save_plot(self):
        folder_prefix = self.output_folder_path + "/simulation-analysis/" + self.metric + "/"
        plt.savefig(
            folder_prefix + "multimodel_metric=" + self.metric + "_window=" + str(self.window_size) + ".png")

    """
    Takes the mean of the chunks, depending on the window size (i.e., chunk size).
    """

    def mean_of_chunks_and_margin_error(self, np_array, window_size):
        chunks = [np_array[i:i + window_size] for i in range(0, len(np_array), window_size)]
        means = [np.mean(chunk) for chunk in chunks]
        errors = [np.std(chunk) / np.sqrt(len(chunk)) for chunk in chunks]
        return np.array(means), np.array(errors)

    """
    Dynamically sets the y limit for the plot, which is 10% higher than the maximum value in the computed data, and 10%
    smaller than the minimum value in the computed data. This is done to ensure that the plot is not too zoomed in or out.
    """

    def get_y_lim(self):
        y_min = min([min(model.processed_host_data - model.margins_of_error) for model in self.models])
        y_max = max([max(model.processed_host_data + model.margins_of_error) for model in self.models])
        return [y_min * 0.95, y_max * 1.05]

    """
    Computes the total of energy consumption / co2 emissions (depending on the input metric)
    for each model.

    !Unit of measurement [Wh] or [gCO2]!

    @return: a list of cumulated energies / emissions for each model (array)
    """

    def get_cumulated(self):
        cumulated_energies = []
        for (i, model) in enumerate(self.models):
            cumulated_energy = model.processed_host_data.sum()
            cumulated_energies.append(cumulated_energy)

        return cumulated_energies

    """
    Computes the average CPU utilization for each model.
    """

    def get_average_cpu_utilization(self):
        average_cpu_utilizations = []
        for model in self.models:
            average_cpu_utilization = model.processed_host_data.mean()
            average_cpu_utilizations.append(average_cpu_utilization)

        return average_cpu_utilizations

    def output_stats(self):
        analysis_file_path = utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/" + self.metric + "/analysis.txt"
        with open(analysis_file_path, "a") as f:
            f.write("\n\n========================================\n")
            f.write("Simulation made at " + time.strftime("%Y-%m-%d %H:%M:%S") + "\n")
            f.write(
                "We are running MultiModel for " + self.metric + ", with window size " + str(self.window_size) + "\n")
            f.write("Sample count in raw host data: " + str(self.max_model_len) + "\n")
