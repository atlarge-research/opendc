import matplotlib.pyplot as plt
import numpy as np
import os
import pyarrow.parquet as pq
from time import time, strftime
from matplotlib.ticker import MaxNLocator, FuncFormatter
from matplotlib.ticker import AutoMinorLocator
from typing import IO
from textwrap import dedent
from models import Model
from util import SimulationConfig, adjust_unit, PlotType, SIMULATION_DATA_FILE


class MultiModel:
    """
    Handles multiple simulation models, aggregates their data based on user-defined parameters,
    and generates plots and statistics.

    Attributes:
        window_size (int): The size of the window for data aggregation, which affects how data smoothing and granularity are handled.
        models (list of Model): A list of Model instances that store the simulation data.
        measure_unit (str): The unit of measurement for the simulation data, adjusted according to the user's specifications.
        unit_scaling (int): The scaling factor applied to the unit of measurement.
        max_model_len (int): The length of the shortest model's raw data, used for consistency in processing.
        plot_path (str): The path where the generated plot will be saved.
        analysis_file (IO): The file object for writing detailed analysis statistics.
        COLOR_PALETTE (list of str): A list of color codes for plotting multiple models.

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

    COLOR_PALETTE: list[str] = [
        # Colorblind-friendly palette
        "#0072B2", "#E69F00", "#009E73", "#D55E00", "#CC79A7", "#F0E442", "#8B4513",
        "#56B4E9", "#F0A3FF", "#FFB400", "#00BFFF", "#90EE90", "#FF6347", "#8A2BE2", "#CD5C5C",
        "#4682B4", "#FFDEAD", "#32CD32", "#D3D3D3", "#999999"
    ]

    def __init__(self, config: SimulationConfig, window_size: int = -1):
        """
        Initializes the MultiModel with provided user settings and prepares the environment.

        :param user_input (dict): Configurations and settings from the user.
        :param path (str): Path where output and analysis will be stored.
        :param window_size (int): The size of the window to aggregate data; uses user input if -1.
        :return: None
        """

        self.config: SimulationConfig = config
        self.starting_time: float = time()
        self.workload_time = None
        self.timestamps = None
        self.plot_path: str | None = None

        self.window_size = config.window_size if window_size == -1 else window_size
        self.measure_unit: str
        self.unit_scaling: int
        self.measure_unit, self.unit_scaling = adjust_unit(config.current_unit, config.unit_scaling_magnitude)

        self.models: list[Model] = []
        self.max_model_len = 0

        try:
            os.makedirs(self.config.output_path, exist_ok=True)
            self.analysis_file: IO = open(config.output_path + "/analysis.txt", "w")
        except Exception as e:
            print(f"Error handling output directory: {e}")
            exit(1)

        self.analysis_file.write("Analysis file create\n")

        self.init_models()
        if self.config.is_metamodel:
            self.COLOR_PALETTE = ["#b3b3b3" for _ in range(len(self.models))]
        if len(self.config.plot_colors) > 0:
            self.COLOR_PALETTE = self.config.plot_colors
        self.compute_windowed_aggregation()

    def get_model_path(self, dir: str) -> str:
        return (
            f"{self.config.simulation_path}/"
            f"{dir}/"
            f"seed={self.config.seed}/"
            f"{SIMULATION_DATA_FILE}.parquet"
        )

    def init_models(self):
        """
        Initializes models from the simulation output stored in Parquet files. This method reads each Parquet file,
        processes the relevant data, and initializes Model instances which are stored in the model list.

        :return: None
        :raise ValueError: If the unit scaling has not been set prior to model initialization.
        """
        if self.unit_scaling is None:
            raise ValueError("Unit scaling factor is not set. Please ensure it is set correctly.")

        simulation_directories = os.listdir(self.config.simulation_path)
        simulation_directories.sort()

        for sim_dir in simulation_directories:
            print("Processing simulation: ", sim_dir)
            if sim_dir == "metamodel":
                continue

            simulation_id: str = os.path.basename(sim_dir)
            columns_to_read = ['timestamp', self.config.metric]
            parquet_file = pq.read_table(self.get_model_path(sim_dir), columns=columns_to_read).to_pandas()

            grouped_data = parquet_file.groupby('timestamp')[self.config.metric].sum()
            # Apply unit scaling to the raw data
            raw = np.divide(grouped_data.values, self.unit_scaling)
            timestamps = parquet_file['timestamp'].unique()

            model = Model(raw_sim_data=raw, identifier=simulation_id)
            self.models.append(model)

            if self.timestamps is None or len(self.timestamps) > len(timestamps):
                self.timestamps = timestamps

        self.max_model_len = min([len(model.raw_sim_data) for model in self.models])

    def compute_windowed_aggregation(self) -> None:
        """
        Applies a windowed aggregation function to each model's dataset. This method is typically used for smoothing
        or reducing data granularity. It involves segmenting the dataset into windows of specified size and applying
        an aggregation function to each segment.

        :return: None
        :side effect: Modifies each model's processed_sim_data attribute to contain aggregated data.
        """
        if self.config.plot_type == PlotType.CUMULATIVE:
            return

        for model in self.models:
            numeric_values = model.raw_sim_data
            model.processed_sim_data = self.mean_of_chunks(numeric_values, self.config.window_size)

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
        plt.figure(figsize=self.config.fig_size)

        plt.xticks(size=32)
        plt.yticks(size=32)
        plt.ylabel(self.config.y_axis.label, size=26)
        plt.xlabel(self.config.x_axis.label, size=26)
        plt.title(self.config.plot_title, size=26)
        plt.grid()

        formatter = FuncFormatter(lambda x, _: '{:,}'.format(int(x)) if x >= 1000 else int(x))
        ax = plt.gca()
        ax.xaxis.set_major_formatter(formatter)

        if self.config.x_axis.has_ticks():
            ax = plt.gca()
            ax.xaxis.set_major_locator(MaxNLocator(self.config.x_axis.ticks))

        if self.config.y_axis.has_ticks():
            ax = plt.gca()
            ax.yaxis.set_major_locator(MaxNLocator(self.config.y_axis.ticks))

        self.set_axis_limits()

        match self.config.plot_type:
            case PlotType.TIME_SERIES:
                self.generate_time_series_plot()
            case PlotType.CUMULATIVE:
                self.generate_cumulative_plot()
            case PlotType.CUMULATIVE_TIME_SERIES:
                self.generate_cumulative_time_series_plot()

        plt.tight_layout()
        plt.subplots_adjust(right=0.85)
        self.save_plot()
        self.output_stats()

    def generate_time_series_plot(self):
        """
        Plots time series data for each model. This function iterates over each model, applies the defined
        windowing function to smooth the data, and plots the resulting series.

        :return: None
        :side effect: Plots are displayed on the matplotlib figure canvas.
        """

        for i, model in enumerate(self.models):
            label = "Meta-Model" if model.is_meta_model() else "Model " + str(model.id)

            if model.is_meta_model():
                repeated_means = np.repeat(model.processed_sim_data, self.window_size)
                plt.plot(repeated_means, drawstyle='steps-mid', label=label, color="#228B22", linestyle="solid",
                         linewidth=2)
            else:
                means = self.mean_of_chunks(model.raw_sim_data, self.window_size)
                repeated_means = np.repeat(means, self.window_size)[:len(model.raw_sim_data)]
                plt.plot(repeated_means, drawstyle='steps-mid', label=label, color=self.COLOR_PALETTE[i])

    def generate_cumulative_plot(self):
        """
        Generates a horizontal bar chart showing cumulative data for each model. This function
        aggregates total values per model and displays them in a bar chart, providing a visual
        comparison of total values across models.

        :return: None
        :side effect: Plots are displayed on the matplotlib figure canvas.
        """
        plt.xlim(self.get_cumulative_limits(model_sums=self.sum_models_entries()))
        plt.ylabel("Model ID", size=30)
        plt.xlabel(self.config.x_axis.label, size=30)

        ax = plt.gca()
        ax.tick_params(axis='x', which='major', length=12)  # Set length of the ticks
        ax.set_xticklabels([])  # Hide x-axis numbers
        ax.xaxis.set_minor_locator(AutoMinorLocator(5))  # Set two minor ticks between majors
        ax.tick_params(axis='x', which='minor', length=7, color='black')
        plt.yticks(range(len(self.models)), [model.id for model in self.models])

        plt.grid(False)

        cumulated_energies = self.sum_models_entries()

        for i, model in (enumerate(self.models)):
            label = "Meta-Model" if model.is_meta_model() else "Model " + str(model.id)
            if model.is_meta_model():
                plt.barh(i, cumulated_energies[i], label=label, color='#009E73', hatch='//')
                plt.text(cumulated_energies[i], i, str(int(round(cumulated_energies[i], 0))), ha='left', va='center',
                         size=26)
            else:
                round_decimals = 0 if cumulated_energies[i] > 500 else 1
                plt.barh(label=label, y=i, width=cumulated_energies[i], color=self.COLOR_PALETTE[i])
                plt.text(cumulated_energies[i], i, str(int(round(cumulated_energies[i], round_decimals))), ha='left',
                         va='center', size=26)

    def generate_cumulative_time_series_plot(self):
        """
        Generates a plot showing the cumulative data over time for each model. This visual representation is
        useful for analyzing trends and the accumulation of values over time.

        :return: None
        :side effect: Displays the cumulative data over time on the matplotlib figure canvas.
        """
        self.compute_cumulative_time_series()

        for i, model in enumerate(self.models):
            label = "Meta-Model" if model.is_meta_model() else "Model " + str(model.id)
            if model.is_meta_model():
                cumulative_repeated = np.repeat(model.cumulative_time_series_values, self.window_size)[
                                      :len(model.processed_sim_data) * self.window_size]
                plt.plot(cumulative_repeated, label=label, drawstyle='steps-mid', color="#228B22", linestyle="solid",
                         linewidth=2)
            else:
                cumulative_repeated = np.repeat(model.cumulative_time_series_values, self.window_size)[
                                      :len(model.raw_sim_data)]
                plt.plot(cumulative_repeated, drawstyle='steps-mid', label=("Model " + str(model.id)),
                         color=self.COLOR_PALETTE[i])

    def compute_cumulative_time_series(self):
        """
        Computes the cumulative sum of processed data over time for each model, storing the result for use in plotting.

        :return: None
        :side effect: Updates each model's 'cumulative_time_series_values' attribute with the cumulative sums.
        """
        for model in self.models:
            cumulative_array = []
            _sum = 0
            for value in model.processed_sim_data:
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
        output_dir = f"{self.config.output_path}/simulation-analysis/{self.config.metric}"
        try:
            os.makedirs(output_dir, exist_ok=True)
        except OSError as e:
            print(f"Error handling output directory: {e}")
            exit(1)

        self.plot_path: str = (
            f"{output_dir}/"
            f"{self.config.plot_type}"
            f"_plot_multimodel_metric={self.config.metric}"
            f"_window={self.window_size}"
            f".pdf"
        ) if self.config.figure_export_name is None \
            else f"{output_dir}/{self.config.figure_export_name}.pdf"

        plt.savefig(self.plot_path)

    def set_axis_limits(self) -> None:
        """
        Sets the x-axis and y-axis limits for the current plot based on the user-defined configuration.
        This method ensures that the plot displays the data within the specified range, enhancing readability.
        """
        if self.config.x_axis.has_range():
            plt.xlim(left=self.config.x_axis.value_range[0], right=self.config.x_axis.value_range[1])

        if self.config.y_axis.has_range():
            plt.ylim(bottom=self.config.y_axis.value_range[0], top=self.config.y_axis.value_range[1])

    def sum_models_entries(self):
        """
        Computes the total values from each model for use in cumulative plotting. This method aggregates
        the data across all models and prepares it for cumulative display.

        :return: List of summed values for each model, useful for plotting and analysis.
        """
        models_sums = []
        for i, model in enumerate(self.models):
            if model.is_meta_model():
                models_sums.append(model.cumulated)
            else:
                cumulated_energy = model.raw_sim_data.sum()
                cumulated_energy = round(cumulated_energy, 2)
                models_sums.append(cumulated_energy)

        return models_sums

    def output_stats(self) -> None:
        """
        Records and writes detailed simulation statistics to an analysis file. This includes time stamps,
        performance metrics, and other relevant details.

        :return: None
        :side effect: Appends detailed simulation statistics to an existing file for record-keeping and analysis.
        """
        end_time: float = time()
        self.analysis_file.write(dedent(
            f"""
            =========================================================
            Simulation made at {strftime("%Y-%m-%d %H:%M:%S")}
            Metric: {self.config.metric}
            Unit: {self.measure_unit}
            Window size: {self.window_size}
            Sample count in raw sim data: {self.max_model_len}
            Computing time {round(end_time - self.starting_time, 1)}s
            Plot path: {self.plot_path}
            =========================================================
            """
        ))

    def mean_of_chunks(self, np_array: np.array, window_size: int) -> np.array:
        """
        Calculates the mean of data within each chunk for a given array. This method helps in smoothing the data by
        averaging over specified 'window_size' segments.

        :param np_array: Array of numerical data to be chunked and averaged.
        :param window_size: The size of each segment to average over.
        :return: np.array: An array of mean values for each chunk.
        """
        if window_size == 1:
            return np_array

        chunks: list[np.array] = [np_array[i:i + window_size] for i in range(0, len(np_array), window_size)]
        means: list[float] = [np.mean(chunk) for chunk in chunks]
        return np.array(means)

    def get_cumulative_limits(self, model_sums: list[float]) -> list[float]:
        """
        Calculates the appropriate x-axis limits for cumulative plots based on the summarized data from each model.

        :param model_sums: List of summed values for each model.
        :return: list[float]: A list containing the minimum and maximum values for the x-axis limits.
        """
        axis_min = min(model_sums) * 0.9
        axis_max = max(model_sums) * 1.1

        if self.config.x_axis.value_range is not None:
            axis_min = self.config.x_axis.value_range[0]
            axis_max = self.config.x_axis.value_range[1]

        return [axis_min * 0.9, axis_max * 1.1]
