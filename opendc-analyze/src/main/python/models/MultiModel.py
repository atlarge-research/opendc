import math
import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import pyarrow.parquet as pq
import time

import utils
from .Model import Model


def is_meta_model(model):
    """
    Check if the given model is a MetaModel based on its ID. A metamodel will always have an id of 101.

    Args:
        model (Model): The model to check.

    Returns:
        bool: True if model is MetaModel, False otherwise.
    """
    return model.id == 101


class MultiModel:
    """
    Handles multiple simulation models, aggregates their data based on user-defined parameters,
    and generates plots and statistics.

    Attributes:
        user_input (dict): Configuration dictionary containing user settings for model processing.
        path (str): The base directory path where output files and analysis results are stored.
        window_size (int): The size of the window for data aggregation, which affects how data smoothing and granularity are handled.
        models (list of Model): A list of Model instances that store the simulation data.
        metric (str): The specific metric to be analyzed and plotted, as defined by the user.
        measure_unit (str): The unit of measurement for the simulation data, adjusted according to the user's specifications.
        output_folder_path (str): Path to the folder where output files are saved.
        raw_output_path (str): Directory path where raw simulation data is stored.
        analysis_file_path (str): Path to the file where detailed analysis results are recorded.
        plot_type (str): The type of plot to generate, which can be 'time_series', 'cumulative', or 'cumulative_time_series'.
        plot_title (str): The title of the plot.
        x_label (str), y_label (str): Labels for the x and y axes of the plot.
        x_min (float), x_max (float), y_min (float), y_max (float): Optional parameters to define axis limits for the plots.

    Methods:
        parse_user_input(window_size): Parses and sets the class attributes based on the provided user input.
        adjust_unit(): Adjusts the unit of measurement based on user settings, applying appropriate metric prefixes.
        set_paths(): Initializes the directory paths for storing outputs and analysis results.
        init_models(): Reads simulation data from Parquet files and initializes Model instances.
        compute_windowed_aggregation(): Processes the raw data by applying a windowed aggregation function for smoothing.
        generate_plot(): Orchestrates the generation of the specified plot type by calling the respective plotting functions.
        generate_time_series_plot(): Generates a time series plot of the aggregated data.
        generate_cumulative_plot(): Creates a bar chart showing cumulative data for each model.
        generate_cumulative_time_series_plot(): Produces a plot that displays cumulative data over time for each model.
        save_plot(): Saves the generated plot to a PDF file in the specified directory.
        output_stats(): Writes detailed statistics of the simulation to an analysis file for record-keeping.
        mean_of_chunks(np_array, window_size): Calculates the mean of data segments for smoothing and processing.
        get_cumulative_limits(model_sums): Determines appropriate x-axis limits for cumulative plots based on the model data.

    Usage:
        To use this class, instantiate it with a dictionary of user settings, a path for outputs, and optionally a window size.
        Call the `generate_plot` method to process the data and generate plots as configured by the user.
    """

    def __init__(self, user_input, path, window_size=-1):
        """
        Initializes the MultiModel with provided user settings and prepares the environment.

        :param user_input (dict): Configurations and settings from the user.
        :param path (str): Path where output and analysis will be stored.
        :param window_size (int): The size of the window to aggregate data; uses user input if -1.
        :return: None
        """

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

    def parse_user_input(self, window_size):
        """
        Parses and sets attributes based on user input.

        :param window_size (int): Specified window size for data aggregation, defaults to user_input if -1.
        :return: None
        """
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

    def adjust_unit(self):
        """
        Adjusts the unit of measurement according to the scaling magnitude specified by the user.
        This method translates the given measurement scale into a scientifically accepted metric prefix.

        :return str: The metric prefixed by the appropriate scale (e.g., 'kWh' for kilo-watt-hour if the scale is 3).
        :raise ValueError: If the unit scaling magnitude provided by the user is not within the accepted range of scaling factors.
        """
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

    def set_paths(self):
        """
        Configures and initializes the directory paths for output and analysis based on the base directory provided.
        This method sets paths for the raw output and detailed analysis results, ensuring directories are created if
        they do not already exist, and prepares a base file for capturing analytical summaries.

        :return: None
        :side effect: Creates necessary directories and files for output and analysis.
        """
        self.output_folder_path = os.getcwd() + "/" + self.path
        self.raw_output_path = os.getcwd() + "/" + self.path + "/raw-output"
        self.analysis_file_path = os.getcwd() + "/" + self.path + "/simulation-analysis/"
        os.makedirs(self.analysis_file_path, exist_ok=True)
        self.analysis_file_path = os.path.join(self.analysis_file_path, "analysis.txt")
        if not os.path.exists(self.analysis_file_path):
            with open(self.analysis_file_path, "w") as f:
                f.write("Analysis file created.\n")

    def init_models(self):
        """
        Initializes models from the simulation output stored in Parquet files. This method reads each Parquet file,
        processes the relevant data, and initializes Model instances which are stored in the model list.

        :return: None
        :raise ValueError: If the unit scaling has not been set prior to model initialization.
        """
        model_id = 0

        for simulation_folder in os.listdir(self.raw_output_path):
            if simulation_folder == "metamodel":
                continue
            path_of_parquet_file = f"{self.raw_output_path}/{simulation_folder}/seed=0/host.parquet"
            parquet_file = pq.read_table(path_of_parquet_file).to_pandas()
            raw_data = parquet_file.select_dtypes(include=[np.number]).groupby("timestamp")
            raw_data = raw_data[self.metric].sum().values

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

    def compute_windowed_aggregation(self):
        """
        Applies a windowed aggregation function to each model's dataset. This method is typically used for smoothing
        or reducing data granularity. It involves segmenting the dataset into windows of specified size and applying
        an aggregation function to each segment.

        :return: None
        :side effect: Modifies each model's processed_host_data attribute to contain aggregated data.
        """
        if self.plot_type != "cumulative":
            for model in self.models:
                numeric_values = model.raw_host_data
                model.processed_host_data = self.mean_of_chunks(numeric_values, self.window_size)

    def generate_plot(self):
        """
        Creates and saves plots based on the processed data from multiple models. This method determines
        the type of plot to generate based on user input and invokes the appropriate plotting function.

        The plotting options supported are 'time_series', 'cumulative', and 'cumulative_time_series'.
        Depending on the type specified, this method delegates to specific plot-generating functions.

        :return: None
        :raises ValueError: If the plot type specified is not recognized or supported by the system.
        :side effect:
            - Generates and saves a plot to the file system.
            - Updates the plot attributes based on the generated plot.
            - Displays the plot on the matplotlib figure canvas.
        """
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

        plt.legend(fontsize=22)
        self.save_plot()
        self.output_stats()

    def generate_time_series_plot(self):
        """
        Plots time series data for each model. This function iterates over each model, applies the defined
        windowing function to smooth the data, and plots the resulting series.

        :return: None
        :side effect: Plots are displayed on the matplotlib figure canvas.
        """
        for model in self.models:
            if not is_meta_model(model):
                means = self.mean_of_chunks(model.raw_host_data, self.window_size)
                repeated_means = np.repeat(means, self.window_size)[:len(model.raw_host_data)]
            else:
                repeated_means = np.repeat(means, self.window_size)[:len(model.processed_host_data) * self.window_size]
            label = "Meta-Model" if is_meta_model(model) else "Model " + str(model.id)
            plt.plot(repeated_means, drawstyle='steps-mid', label=label)

    def generate_cumulative_plot(self):
        """
        Generates a horizontal bar chart showing cumulative data for each model. This function
        aggregates total values per model and displays them in a bar chart, providing a visual
        comparison of total values across models.

        :return: None
        :side effect: Plots are displayed on the matplotlib figure canvas.
        """
        plt.xlim(self.get_cumulative_limits(model_sums=self.sum_models_entries()))
        plt.ylabel("Model ID", size=20)
        plt.xlabel("Total " + self.metric + " [" + self.measure_unit + "]")
        plt.yticks(range(len(self.models)), [model.id for model in self.models])
        plt.grid(False)

        cumulated_energies = self.sum_models_entries()
        for i, model in enumerate(self.models):
            label = "Meta-Model" if is_meta_model(model) else "Model " + str(model.id)
            plt.barh(label=label, y=i, width=cumulated_energies[i])
            plt.text(cumulated_energies[i], i, str(cumulated_energies[i]), ha='left', va='center', size=26)

    def generate_cumulative_time_series_plot(self):
        """
        Generates a plot showing the cumulative data over time for each model. This visual representation is
        useful for analyzing trends and the accumulation of values over time.

        :return: None
        :side effect: Displays the cumulative data over time on the matplotlib figure canvas.
        """
        self.compute_cumulative_time_series()

        for model in self.models:
            if is_meta_model(model):
                cumulative_repeated = np.repeat(model.cumulative_time_series_values, self.window_size)[
                                      :len(model.processed_host_data) * self.window_size]
            else:
                cumulative_repeated = np.repeat(model.cumulative_time_series_values, self.window_size)[
                                      :len(model.raw_host_data)]
            plt.plot(cumulative_repeated, drawstyle='steps-mid', label=("Model " + str(model.id) + " cumulative"))

    def compute_cumulative_time_series(self):
        """
        Computes the cumulative sum of processed data over time for each model, storing the result for use in plotting.

        :return: None
        :side effect: Updates each model's 'cumulative_time_series_values' attribute with the cumulative sums.
        """
        for model in self.models:
            cumulative_array = []
            _sum = 0
            for value in model.processed_host_data:
                _sum += value
                cumulative_array.append(_sum * self.window_size)
            model.cumulative_time_series_values = cumulative_array

    def save_plot(self):
        """
        Saves the current plot to a PDF file in the specified directory, constructing the file path from the
        plot attributes and ensuring that the directory exists before saving.

        :return: None
        :side effect: Creates or overwrites a PDF file containing the plot in the designated folder.
        """
        folder_prefix = self.output_folder_path + "/simulation-analysis/" + self.metric + "/"
        self.plot_path = folder_prefix + self.plot_type + "_plot_multimodel_metric=" + self.metric + "_window=" + str(
            self.window_size) + ".pdf"
        plt.savefig(self.plot_path)

    def set_x_axis_lim(self):
        """
        Sets the x-axis limits for the plot based on user-defined minimum and maximum values. If values
        are not specified, the axis limits will default to encompassing all data points.

        :return: None
        :side effect: Adjusts the x-axis limits of the current matplotlib plot.
        """
        if self.x_min is not None:
            plt.xlim(left=self.x_min)

        if self.x_max is not None:
            plt.xlim(right=self.x_max)

    def set_y_axis_lim(self):
        """
        Dynamically sets the y-axis limits to be slightly larger than the range of the data, enhancing
        the readability of the plot by ensuring all data points are comfortably within the view.

        :return: None
        :side effect: Adjusts the y-axis limits of the current matplotlib plot.
        """
        if self.y_min is not None:
            plt.ylim(bottom=self.y_min)
        if self.y_max is not None:
            plt.ylim(top=self.y_max)

    def sum_models_entries(self):
        """
        Computes the total values from each model for use in cumulative plotting. This method aggregates
        the data across all models and prepares it for cumulative display.

        :return: List of summed values for each model, useful for plotting and analysis.
        """
        models_sums = []
        for (i, model) in enumerate(self.models):
            if is_meta_model(model):
                models_sums.append(model.cumulated)
            else:
                cumulated_energy = model.raw_host_data.sum()
                cumulated_energy = round(cumulated_energy, 2)
                models_sums.append(cumulated_energy)

        return models_sums

    def output_stats(self):
        """
        Records and writes detailed simulation statistics to an analysis file. This includes time stamps,
        performance metrics, and other relevant details.

        :return: None
        :side effect: Appends detailed simulation statistics to an existing file for record-keeping and analysis.
        """
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
        """
        Calculates the mean of data within each chunk for a given array. This method helps in smoothing the data by
        averaging over specified 'window_size' segments.

        :param np_array (np.array): Array of numerical data to be chunked and averaged.
        :param window_size (int): The size of each segment to average over.
        :return: np.array: An array of mean values for each chunk.
        :side effect: None
        """
        if window_size == 1:
            return np_array

        chunks = [np_array[i:i + window_size] for i in range(0, len(np_array), window_size)]
        means = [np.mean(chunk) for chunk in chunks]
        return np.array(means)

    def get_cumulative_limits(self, model_sums):
        """
        Calculates the appropriate x-axis limits for cumulative plots based on the summarized data from each model.

        :param model_sums (list of float): The total values for each model.
        :return: tuple: A tuple containing the minimum and maximum x-axis limits.
        """
        axis_min = min(model_sums) * 0.9
        axis_max = max(model_sums) * 1.1

        if self.user_input["x_min"] is not None:
            axis_min = self.user_input["x_min"]
        if self.user_input["x_max"] is not None:
            axis_max = self.user_input["x_max"]

        return [axis_min * 0.9, axis_max * 1.1]
