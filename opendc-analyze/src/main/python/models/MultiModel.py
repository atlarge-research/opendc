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
    def __init__(self, user_input, path):
        self.user_input = user_input

        self.metric = None
        self.measure_unit = None
        self.path = path
        self.models = []

        self.folder_path = None
        self.output_folder_path = None
        self.raw_output_path = None
        self.analysis_file_path = None
        self.unit_scaling_factor = 1
        self.window_size = -1
        self.aggregation_function = "median"
        self.workload_time = 0
        self.max_model_len = 0

        self.plot_type = None
        self.plot_title = None
        self.x_label = None
        self.y_label = None
        self.y_min = None
        self.y_max = None

        # run init functions
        self.parse_user_input()
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
        self.measure_unit = self.adjust_unit()
        self.aggregation_function = self.user_input["aggregation_function"]

        self.plot_type = self.user_input["plot_type"]
        self.plot_title = self.user_input["plot_title"]
        self.x_label = self.user_input["x_label"]
        self.y_label = self.user_input["y_label"]
        self.y_min = self.user_input["y_min"]
        self.y_max = self.user_input["y_max"]

    """
    This function matches the prefixes with the scaling factors. The prefixes are used to adjust the unit of measurement.
    "n" for nano, "μ" for micro, "m" for milli, "" for unit, "k" for kilo, "M" for mega, "G" for giga, "T" for tera.
    """

    def adjust_unit(self):
        prefixes = ['n', 'μ', 'm', '', 'k', 'M', 'G', 'T']
        scaling_factors = [10 ** -9, 10 ** -6, 10 ** -3, 1, 10 ** 3, 10 ** 6, 10 ** 9, 10 ** 12]
        given_metric = self.user_input["current_unit"]
        self.unit_scaling_factor = self.user_input["unit_scaling_factor"]

        if self.unit_scaling_factor not in scaling_factors:
            raise ValueError(
                "Unit scaling factor not found. Please enter a valid unit from [10^-9, 10^-6, 10^-3, 1, 10^3, 10^6, 10^9, 10^12].")

        for i in range(len(scaling_factors)):
            if self.unit_scaling_factor == scaling_factors[i]:
                result = prefixes[i] + given_metric
                return result

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

            if self.unit_scaling_factor is None:
                raise ValueError("Unit scaling factor is not set. Please ensure it is set correctly.")

            raw_data = np.divide(raw_data, self.unit_scaling_factor)
            raw_data = raw_data.round(2)

            if self.user_input["samples_per_minute"] > 0:
                total_values = len(raw_data)
                total_time = total_values * self.user_input["samples_per_minute"] / 60 / 24
                print("There are " + str(
                    total_values) + " values in the raw data, hence the data is measured for a time of"
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
        for model in self.models:
            numeric_values = model.raw_host_data
            model.processed_host_data, model.margins_of_error = self.mean_of_chunks_and_margin_error(numeric_values, self.window_size)

    """
    Generates plot for the MultiModel from the already computed data. The plot is saved in the analysis folder.
    """

    def generate_plot(self):
        if self.plot_type == "time_series":
            self.generate_time_series_plot()
        elif self.plot_type == "cumulative_total":
            self.generate_cumulative_plot()
        elif self.plot_type == "cumulative_time_series":
            self.generate_cumulative_time_series_plot()
        else:
            raise ValueError(
                "Plot type not recognized. Please enter a valid plot type. The plot can be either "
                "'time_series', 'cumulative_total', or 'cumulative_time_series'."
            )

        self.save_plot()

    def generate_time_series_plot(self):
        plt.figure(figsize=(20, 10))
        plt.title(self.plot_title)
        plt.xlabel(self.x_label)
        plt.ylim(self.get_axis_lim())
        plt.ylabel(self.metric + " " + self.measure_unit)
        plt.grid()
        for model in self.models:
            plt.plot(model.processed_host_data, label=("Model " + str(model.id) + "-" + model.experiment_name))

        plt.legend()

    def generate_cumulative_plot(self):
        plt.figure(figsize=(20, 10))
        plt.title(self.plot_title)
        plt.ylabel(self.x_label)
        plt.xlim(self.get_cumulative_limits(model_sums=self.sum_models_entries()))

        plt.xlabel(self.metric + " [" + self.measure_unit + "]", size=20)
        plt.ylabel("Model ID", size=20)
        plt.yticks(range(len(self.models)), [model.id for model in self.models], size=16)
        plt.xticks(size=16)

        cumulated_energies = self.sum_models_entries()

        for i, model in enumerate(self.models):
            plt.barh(label=("Model " + str(model.id)), y=i, width=cumulated_energies[i])
            plt.text(cumulated_energies[i], i, str(cumulated_energies[i]), ha='left', va='center', size=16)

    def generate_cumulative_time_series_plot(self):
        self.compute_cumulative_time_series()

        plt.figure(figsize=(20, 10))
        plt.title(self.plot_title)
        plt.xlabel(self.x_label)

        for i, model in enumerate(self.models):
            print(model.time_cumulative)
            plt.plot(model.time_cumulative)

    """
    Save the plot in the analysis folder.
    """

    def compute_cumulative_time_series(self):
        sum = 0
        for model in self.models:
            cumulative_array = []
            sum = 0
            for i in range(len(model.processed_host_data)):
                sum += model.processed_host_data[i]
                cumulative_array.append(sum)
            model.time_cumulative = cumulative_array

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

    def get_cumulative_limits(self, model_sums):
        axis_min = min(model_sums)
        axis_max = max(model_sums)
        return [axis_min * 0.9, axis_max * 1.1]

    """
    Dynamically sets the y limit for the plot, which is 10% higher than the maximum value in the computed data, and 10%
    smaller than the minimum value in the computed data. This is done to ensure that the plot is not too zoomed in or out.
    """

    def get_axis_lim(self):
        axis_min = min([min(model.processed_host_data - model.margins_of_error) for model in self.models])
        axis_max = max([max(model.processed_host_data + model.margins_of_error) for model in self.models])
        return [axis_min * 0.95, axis_max * 1.05]

    """
    Computes the total of energy consumption / co2 emissions (depending on the input metric)
    for each model.

    !Unit of measurement [Wh] or [gCO2]!

    @return: a list of cumulated energies / emissions for each model (array)
    """

    def sum_models_entries(self):
        models_sums = []
        for (i, model) in enumerate(self.models):
            cumulated_energy = model.processed_host_data.sum()
            cumulated_energy = round(cumulated_energy, 2)

            models_sums.append(cumulated_energy)

        return models_sums

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
