import math
import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import pyarrow.parquet as pq
import time

import utils
from .Model import Model

def isMetaModel(model):
    return model.id == 101

class MultiModel:
    def __init__(self, user_input, path, window_size=-1):
        self.starting_time = time.time()
        self.end_time = None
        self.workload_time = None

        self.user_input = user_input

        self.metric = None
        self.measure_unit = None
        self.path = path
        self.models = []

        self.folder_path = None
        self.output_folder_path = None
        self.raw_output_path = None
        self.analysis_file_path = None
        self.unit_scaling = 1
        self.window_size = -1
        self.aggregation_function = "median"
        self.max_model_len = 0

        self.plot_type = None
        self.plot_title = None
        self.x_label = None
        self.y_label = None
        self.x_min = None
        self.x_max = None
        self.y_min = None
        self.y_max = None
        self.plot_path = None

        # run init functions
        self.parse_user_input(window_size)
        self.set_paths()
        self.init_models()

        self.compute_windowed_aggregation()

    """
    This function is used to parse the user input. It takes the inputs from the user and sets the attributed of the
    Multi-Model.
    """

    def parse_user_input(self, window_size):
        if window_size == -1:
            self.window_size = self.user_input["window_size"]
        else:
            self.window_size = window_size
        self.metric = self.user_input["metric"]
        self.measure_unit = self.adjust_unit()
        self.aggregation_function = self.user_input["aggregation_function"]

        self.plot_type = self.user_input["plot_type"]
        self.plot_title = self.user_input["plot_title"]
        if self.user_input["x_label"] == "":
            self.x_label = "Samples"
        else:
            self.x_label = self.user_input["x_label"]

        if self.user_input["y_label"] == "":
            self.y_label = self.metric + " [" + self.measure_unit + "]"
        else:
            self.y_label = self.user_input["y_label"]

        self.y_min = self.user_input["y_min"]
        self.y_max = self.user_input["y_max"]
        self.x_min = self.user_input["x_min"]
        self.x_max = self.user_input["x_max"]

    """
    This function matches the prefixes with the scaling factors. The prefixes are used to adjust the unit of measurement.
    "n" for nano, "μ" for micro, "m" for milli, "" for unit, "k" for kilo, "M" for mega, "G" for giga, "T" for tera.
    """

    def adjust_unit(self):
        prefixes = ['n', 'μ', 'm', '', 'k', 'M', 'G', 'T']
        scaling_factors = [-9, -6, -3, 1, 3, 6, 9]
        given_metric = self.user_input["current_unit"]
        self.unit_scaling = self.user_input["unit_scaling_magnitude"]

        if self.unit_scaling not in scaling_factors:
            raise ValueError(
                "Unit scaling factor not found. Please enter a valid unit from [-9, -6, -3, 1, 3, 6, 9].")

        if self.unit_scaling == 1:
            return given_metric

        for i in range(len(scaling_factors)):
            if self.unit_scaling == scaling_factors[i]:
                self.unit_scaling = 10 ** self.unit_scaling
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

            if self.unit_scaling is None:
                raise ValueError("Unit scaling factor is not set. Please ensure it is set correctly.")

            raw_data = np.divide(raw_data, self.unit_scaling)

            if self.user_input["samples_per_minute"] > 0:
                MINUTES_IN_DAY = 1440
                self.workload_time = len(raw_data) * self.user_input["samples_per_minute"] / MINUTES_IN_DAY

            model = Model(raw_host_data=raw_data, id=model_id, path=self.output_folder_path)
            self.models.append(model)
            model_id += 1

        self.max_model_len = min([len(model.raw_host_data) for model in self.models])

    """
    The MultiModel uses a "windowed aggregation" technique to aggregate the data using a window size and a function. This
    technique is similar to a convolution / moving average, which takes chunks of data and aggregates (e.g., average).
    The size of the window to aggregate the data (e.g., an array of 1000 elements, windowed with window_size=10, would
    result in 100 elements)
    """

    def compute_windowed_aggregation(self):
        if self.plot_type != "cumulative":
            for model in self.models:
                numeric_values = model.raw_host_data
                model.processed_host_data = self.mean_of_chunks(numeric_values, self.window_size)

    """
    Generates plot for the MultiModel from the already computed data. The plot is saved in the analysis folder.
    """

    def generate_plot(self):
        plt.figure(figsize=(10, 10))
        plt.xticks(size=22)
        plt.yticks(size=22)
        plt.ylabel(self.y_label, size=26)
        plt.xlabel(self.x_label, size=26)
        plt.title(self.plot_title, size=26)
        plt.grid()

        self.set_x_axis_lim()
        self.set_y_axis_lim()

        if self.plot_type == "time_series":
            self.generate_time_series_plot()
        elif self.plot_type == "cumulative":
            self.generate_cumulative_plot()
        elif self.plot_type == "cumulative_time_series":
            self.generate_cumulative_time_series_plot()
        else:
            raise ValueError(
                "Plot type not recognized. Please enter a valid plot type. The plot can be either "
                "'time_series', 'cumulative', or 'cumulative_time_series'."
            )

        # make legend 3 times bigger
        plt.legend(fontsize=22)
        self.save_plot()
        self.output_stats()

    def generate_time_series_plot(self):

        for model in self.models:
            if not isMetaModel(model):
                means = self.mean_of_chunks(model.raw_host_data, self.window_size)
                repeated_means = np.repeat(means, self.window_size)[:len(model.raw_host_data)]
            else:
                repeated_means = np.repeat(means, self.window_size)[:len(model.raw_host_data) * self.window_size]
            label = "Meta-Model" if isMetaModel(model) else "Model " + str(model.id)
            plt.plot(repeated_means, drawstyle='steps-mid', label=label)

    def generate_cumulative_plot(self):
        plt.xlim(self.get_cumulative_limits(model_sums=self.sum_models_entries()))
        plt.ylabel("Model ID", size=20)
        plt.xlabel("Total " + self.metric + " [" + self.measure_unit + "]")
        plt.yticks(range(len(self.models)), [model.id for model in self.models])
        plt.grid(False)

        cumulated_energies = self.sum_models_entries()
        for i, model in enumerate(self.models):
            label = "Meta-Model" if isMetaModel(model) else "Model " + str(model.id)
            plt.barh(label=label, y=i, width=cumulated_energies[i])
            plt.text(cumulated_energies[i], i, str(cumulated_energies[i]), ha='left', va='center', size=26)

    def generate_cumulative_time_series_plot(self):
        self.compute_cumulative_time_series()

        for model in self.models:
            cumulative_repeated = np.repeat(model.cumulative_time_series_values, self.window_size)[:len(model.raw_host_data)]
            plt.plot(cumulative_repeated, drawstyle='steps-mid', label=("Model " + str(model.id) + " cumulative"))

    """
    Save the plot in the analysis folder.
    """

    def compute_cumulative_time_series(self):
        for model in self.models:
            cumulative_array = []
            _sum = 0
            for value in model.processed_host_data:
                _sum += value
                cumulative_array.append(_sum * self.window_size)
            model.cumulative_time_series_values = cumulative_array

    def save_plot(self):
        folder_prefix = self.output_folder_path + "/simulation-analysis/" + self.metric + "/"
        self.plot_path = folder_prefix + self.plot_type + "_plot_multimodel_metric=" + self.metric + "_window=" + str(
            self.window_size) + ".pdf"
        plt.savefig(self.plot_path)

    def set_x_axis_lim(self):
        if self.x_min is not None:
            plt.xlim(left=self.x_min)

        if self.x_max is not None:
            plt.xlim(right=self.x_max)

    """
    Dynamically sets the y limit for the plot, which is 10% higher than the maximum value in the computed data, and 10%
    smaller than the minimum value in the computed data. This is done to ensure that the plot is not too zoomed in or out.
    """

    def set_y_axis_lim(self):
        if self.y_min is not None:
            plt.ylim(bottom=self.y_min)
        if self.y_max is not None:
            plt.ylim(top=self.y_max)

    """
    Computes the total of energy consumption / co2 emissions (depending on the input metric)
    for each model.

    !Unit of measurement [Wh] or [gCO2]!

    @return: a list of cumulated energies / emissions for each model (array)
    """

    def sum_models_entries(self):
        models_sums = []
        for (i, model) in enumerate(self.models):
            if isMetaModel(model):
                models_sums.append(model.cumulated)
            else:
                cumulated_energy = model.raw_host_data.sum()
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
        self.end_time = time.time()
        with open(self.analysis_file_path, "a") as f:
            f.write("\n\n========================================\n")
            f.write("Simulation made at " + time.strftime("%Y-%m-%d %H:%M:%S") + "\n")
            f.write("Metric: " + self.metric + "\n")
            f.write("Unit: " + self.measure_unit + "\n")
            f.write("Window size: " + str(self.window_size) + "\n")
            f.write("Sample count in raw host data: " + str(self.max_model_len) + "\n")
            f.write("Computing time " + str(round(self.end_time - self.starting_time, 1)) + "s\n")
            if (self.user_input["samples_per_minute"] > 0):
                f.write("Workload time: " + str(round(self.workload_time, 2)) + " days\n")
            f.write("Plot path" + self.plot_path + "\n")
            f.write("========================================\n")

    def mean_of_chunks(self, np_array, window_size):
        if window_size == 1:
            return np_array

        chunks = [np_array[i:i + window_size] for i in range(0, len(np_array), window_size)]
        means = [np.mean(chunk) for chunk in chunks]
        return np.array(means)

    def get_cumulative_limits(self, model_sums):
        axis_min = min(model_sums) * 0.9
        axis_max = max(model_sums) * 1.1

        if self.user_input["x_min"] is not None:
            axis_min = self.user_input["x_min"]
        if self.user_input["x_max"] is not None:
            axis_max = self.user_input["x_max"]

        return [axis_min * 0.9, axis_max * 1.1]
