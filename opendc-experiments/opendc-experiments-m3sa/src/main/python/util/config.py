from json import JSONDecodeError, load
from warnings import warn
from numpy import mean, median
from typing import Callable
from enum import Enum
from sys import stderr
import os

FUNCTIONS = {
    "mean": mean,
    "median": median,
}


class PlotType(Enum):
    TIME_SERIES = "time_series"
    CUMULATIVE = "cumulative"
    CUMULATIVE_TIME_SERIES = "cumulative_time_series"

    def __str__(self) -> str:
        return self.value


def get_plot_type(plot_type: str) -> PlotType:
    """
    Returns the PlotType enum value for the given string
    Args:
        plot_type: the string representation of the plot type
    Returns:
        the PlotType enum value
    """
    return next((pt for pt in PlotType if pt.value == plot_type), PlotType.TIME_SERIES)


class PlotAxis:
    """
    This class represents an axis of a plot. It contains the label, value range, and number of ticks for the axis.
    Attributes:
        label (str): the label of the axis
        value_range (tuple[float, float]): the range of values for the axis
        ticks (int): the number of ticks on the axis
    """

    def __init__(self, label: str, value_range: tuple[float, float] | None, ticks: int | None):
        self.label = label
        self.value_range = value_range
        self.ticks = ticks

    def has_range(self) -> bool:
        """
        Checks if the axis has a value range
        Returns:
            True if the axis has a value range, False otherwise
        """
        return self.value_range is not None

    def has_ticks(self) -> bool:
        """
        Checks if the axis has a number of ticks
        Returns:
            True if the axis has a number of ticks, False otherwise
        """
        return self.ticks is not None


class SimulationConfig:
    """
    This class represents the configuration of a simulation.
    It contains all the necessary parameters to run a simulation using multiple models.

    Attributes:
        is_multimodel (bool): whether the simulation is multimodel
        is_metamodel (bool): whether the simulation is a metamodel
        metric (str): the metric to be used
        window_function (function): the window function to be used
        meta_function (function): the meta function to be used
        window_size (int): the window size
        samples_per_minute (int): the number of samples per minute
        current_unit (str): the current unit
        unit_scaling_magnitude (int): the unit scaling magnitude
        plot_type (str): the plot type
        plot_title (str): the plot title
        x_axis (PlotAxis): the x-axis
        y_axis (PlotAxis): the y-axis
        seed (int): the seed
        fig_size (tuple[int, int]): the figure size
    """

    def __init__(self, input_json: dict[str, any], output_path: str, simulation_path: str):
        """
        Initializes the SimulationConfig object with the given input JSON
        Args:
            input_json: the input JSON object
        Raises:
            ValueError: if the input JSON is missing required
                        fields or has invalid values for certain fields
        """

        if "metric" not in input_json:
            raise ValueError("Required field 'metric' is missing.")
        if "meta_function" not in input_json and input_json["metamodel"]:
            raise ValueError(
                "Required field 'meta_function' is missing. Please select between 'mean' and 'median'. "
                "Alternatively, disable metamodel in the config file."
            )
        if input_json["meta_function"] not in FUNCTIONS:
            raise ValueError(
                "Invalid value for meta_function. Please select between 'mean' and 'median'."
            )
        if "multimodel" not in input_json and input_json["metamodel"]:
            warn("Warning: Missing 'multimodel' field. Defaulting to 'True'.")

        self.output_path: str = output_path
        self.simulation_path: str = simulation_path
        self.is_multimodel: bool = input_json.get("multimodel", True)
        self.is_metamodel: bool = input_json.get("metamodel", False)
        self.metric: str = input_json["metric"]
        self.window_function: Callable[[any], float] = FUNCTIONS[input_json.get("window_function", "mean")]
        self.meta_function: Callable[[any], float] = FUNCTIONS[input_json.get("meta_function", "mean")]
        self.window_size: int = input_json.get("window_size", 1)
        self.samples_per_minute: int = input_json.get("samples_per_minute", 0)
        self.current_unit: str = input_json.get("current_unit", "")
        self.unit_scaling_magnitude: int = input_json.get("unit_scaling_magnitude", 1)
        self.plot_type: PlotType = next(
            (pt for pt in PlotType if pt.value == input_json.get("plot_type", "time_series")), PlotType.TIME_SERIES)
        self.plot_title: str = input_json.get("plot_title", "")
        self.x_axis: PlotAxis = PlotAxis(
            input_json.get("x_label", ""),
            parse_range(input_json, "x"),
            input_json.get("x_ticks_count", None)
        )
        self.y_axis: PlotAxis = PlotAxis(
            input_json.get("y_label", ""),
            parse_range(input_json, "y"),
            input_json.get("y_ticks_count", None)
        )
        self.seed: int = input_json.get("seed", 0)
        self.fig_size: tuple[int, int] = input_json.get("figsize", (20, 10))
        self.plot_colors: list[str] = input_json.get("plot_colors", [])
        self.figure_export_name: str | None = input_json.get("figure_export_name", None)


def parse_range(user_input: dict[str, any], key: str) -> tuple[float, float] | None:
    """
    Parses a range from the user input
    Args:
        user_input: the user input dictionary
        key: the key of the range

    Returns:
        a tuple containing the minimum and maximum values of the range
    """

    if f"{key}_min" not in user_input or f"{key}_max" not in user_input:
        return None

    return user_input[f"{key}_min"], user_input[f"{key}_max"]


def parse_configuration(config_path: str, output_path: str, simulation_path: str) -> SimulationConfig:
    """
    Reads the input JSON file and returns a SimulationConfig object
    Args:
        config_path: the path to the input JSON file
        output_path: the path to the output folder
        simulation_path: the path to the simulation folder

    Returns:
        a SimulationConfig object
    """

    try:
        with (open(config_path, 'r') as json):
            input_json: dict[str, any] = load(json)
    except JSONDecodeError:
        stderr.write(f"Error decoding JSON in file: {config_path}")
        exit(1)
    except IOError:
        stderr.write(f"Error reading file: {config_path}")
        exit(1)

    try:
        return SimulationConfig(input_json, output_path, simulation_path)
    except ValueError as err:
        print(f"Error parsing input JSON: {err}")
        exit(1)
