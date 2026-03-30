import os
import pandas as pd
from models import Model, MultiModel
from typing import Callable
from util import PlotType


class MetaModel:
    """
    A class that aggregates results from multiple simulation models based on user-defined functions, producing
    consolidated outputs for analysis.

    Attributes:
        multi_model (MultiModel): The container of models whose results are aggregated.
        meta_model (Model): Model instance that stores aggregated results.
        meta_function (function): Function used to calculate aggregated data.
        min_raw_model_len (int): Minimum length of raw data arrays across all models.
        min_processed_model_len (int): Minimum length of processed data arrays across all models.
        number_of_models (int): Number of models being aggregated.
        function_map (dict): Mapping of aggregation function names to function implementations.
    """

    META_MODEL_ID = 'M'

    def __init__(self, multi_model: MultiModel, meta_function: Callable[[any], float] = None):
        """
        Initializes the Metamodel with a MultiModel instance and prepares aggregation functions based on configuration.

        :param multi_model: MultiModel instance containing the models to aggregate.
        :raise ValueError: If metamodel functionality is not enabled in the configuration.
        """
        if not multi_model.config.is_metamodel:
            raise ValueError("Metamodel is not enabled in the config file")

        self.multi_model = multi_model
        self.meta_model: Model = Model(
            raw_sim_data=[],
            identifier=self.META_MODEL_ID,
        )

        self.meta_function: Callable[
            [any], float] = self.multi_model.config.meta_function if meta_function is None else meta_function

        self.min_raw_model_len = min([len(model.raw_sim_data) for model in self.multi_model.models])
        self.min_processed_model_len = min([len(model.processed_sim_data) for model in self.multi_model.models])
        self.number_of_models = len(self.multi_model.models)

    def output(self) -> None:
        """
        Generates outputs by plotting the aggregated results and exporting the metamodel data to a file.
        :return: None
        :side effect: Outputs data to files and generates plots.
        """
        self.plot()
        self.output_metamodel()

    def compute(self) -> None:
        """
        Computes aggregated data based on the specified plot type from the configuration.
        :raise ValueError: If an unsupported plot type is specified in the configuration.
        """
        match self.multi_model.config.plot_type:
            case PlotType.TIME_SERIES:
                self.compute_time_series()
            case PlotType.CUMULATIVE:
                self.compute_cumulative()
            case PlotType.CUMULATIVE_TIME_SERIES:
                self.compute_cumulative_time_series()

    def plot(self) -> None:
        """
        Plots the aggregated data according to the specified plot type from the configuration.
        :raise ValueError: If an unsupported plot type is specified.
        """

        match self.multi_model.config.plot_type:
            case PlotType.TIME_SERIES:
                self.plot_time_series()
            case PlotType.CUMULATIVE:
                self.plot_cumulative()
            case PlotType.CUMULATIVE_TIME_SERIES:
                self.plot_cumulative_time_series()

    def compute_time_series(self):
        """
        Aggregates time series data across models using the specified aggregation function.
        :return: None
        :side effect: Updates the meta_model's processed data with aggregated results.
        """
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for model in self.multi_model.models:
                data_entries.append(model.processed_sim_data[i])
            self.meta_model.processed_sim_data.append(self.meta_function(data_entries))
        self.meta_model.raw_sim_data = self.meta_model.processed_sim_data

    def plot_time_series(self):
        """
        Generates a time series plot of the aggregated data.
        :return: None
        :side effect: Displays a time series plot using the multi_model's plotting capabilities.
        """
        self.multi_model.models.append(self.meta_model)
        self.multi_model.generate_plot()

    def compute_cumulative(self):
        """
        Aggregates cumulative data entries across all models.
        :return: None
        :side effect: Updates the meta_model's cumulative data with aggregated results.
        """
        for i in range(0, self.min_raw_model_len):
            data_entries = []
            for model in self.multi_model.models:
                sim_data = model.raw_sim_data
                ith_element = sim_data[i]
                data_entries.append(ith_element)
            self.meta_model.cumulated += self.meta_function(data_entries)

        self.meta_model.cumulated = round(self.meta_model.cumulated, 2)

    def plot_cumulative(self):
        """
        Generates a cumulative plot of the aggregated data.
        :return: None
        :side effect: Displays a cumulative plot using the multi_model's plotting capabilities.
        """
        self.multi_model.models.append(self.meta_model)
        self.multi_model.generate_plot()

    def compute_cumulative_time_series(self):
        """
        Aggregates cumulative time series data entries across models using the specified aggregation function.
        :return: None
        :side effect: Updates the meta_model's processed data with cumulative aggregated results.
        """
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for model in self.multi_model.models:
                data_entries.append(model.processed_sim_data[i])
            self.meta_model.processed_sim_data.append(self.meta_function(data_entries))

    def plot_cumulative_time_series(self):
        """
        Generates a cumulative time series plot of the aggregated data.
        :return: None
        :side effect: Displays a cumulative time series plot using the multi_model's plotting capabilities.
        """
        self.multi_model.models.append(self.meta_model)
        self.multi_model.generate_plot()

    def output_metamodel(self):
        """
        Exports the processed sim data of the metamodel to a parquet file for further analysis or record keeping.
        :return: None
        :side effect: Writes data to a parquet file at the specified directory path.
        """
        directory_path = os.path.join(self.multi_model.config.output_path, "raw-output/metamodel/seed=0")
        try:
            os.makedirs(directory_path, exist_ok=True)
        except OSError as e:
            print(f"Error creating directory: {e}")
            exit(1)

        current_path = os.path.join(directory_path, f"{self.multi_model.config.metric}.parquet")
        minimum = min(len(self.multi_model.timestamps), len(self.meta_model.processed_sim_data))

        df = pd.DataFrame({
            "timestamp": self.multi_model.timestamps[:minimum],
            self.multi_model.config.metric: self.meta_model.processed_sim_data[:minimum]
        })
        df.to_parquet(current_path, index=False)
