import numpy as np
import pandas as pd
import os

import utils
from .Model import Model

META_MODEL_ID = 101


class Metamodel:
    """
    Aggregates results from multiple models into a consolidated output based on specified functions.

    Parameters:
        multimodel (MultiModel): Container of models whose results are aggregated.

    Attributes:
        meta_model (Model): Stores aggregated results.
        meta_simulation_function (function): Function to calculate aggregated data.
        min_raw_model_len (int): Minimum length of raw data across all models.
        min_processed_model_len (int): Minimum length of processed data across all models.
        number_of_models (int): Number of models aggregated.
    """

    def __init__(self, multimodel):
        """
        Initializes the Metamodel with configuration settings and prepares aggregation functions.

        Raises:
            ValueError: If metamodel functionality is disabled in configuration.
        """
        if not multimodel.user_input.get('metamodel', False):
            raise ValueError("Metamodel is not enabled in the config file")

        self.function_map = {
            'mean': self.mean,
            'median': self.median,
            'equation1': self.meta_equation1,
        }

        self.multi_model = multimodel
        self.meta_model = Model(
            raw_host_data=[],
            id=META_MODEL_ID,
            path=self.multi_model.output_folder_path
        )

        self.meta_simulation_function = self.function_map.get(multimodel.user_input['meta_simulation_function'],
                                                              self.mean)
        self.min_raw_model_len = min([len(model.raw_host_data) for model in self.multi_model.models])
        self.min_processed_model_len = min([len(model.processed_host_data) for model in self.multi_model.models])
        self.number_of_models = len(self.multi_model.models)
        self.compute()

    def output(self):
        """Generates output by plotting results and exporting the metamodel data."""
        self.plot()
        self.output_metamodel()

    def compute(self):
        """Computes aggregated data based on the user-specified plot type."""
        if self.multi_model.user_input['plot_type'] == 'time_series':
            self.compute_time_series()
        elif self.multi_model.user_input['plot_type'] == 'cumulative':
            self.compute_cumulative()
        elif self.multi_model.user_input['plot_type'] == 'cumulative_time_series':
            self.compute_cumulative_time_series()
        else:
            raise ValueError("Invalid plot type in config file")

    def plot(self):
        """Plots the aggregated data based on the specified plot type."""
        if self.multi_model.user_input['plot_type'] == 'time_series':
            self.plot_time_series()
        elif self.multi_model.user_input['plot_type'] == 'cumulative':
            self.plot_cumulative()
        elif self.multi_model.user_input['plot_type'] == 'cumulative_time_series':
            self.plot_cumulative_time_series()

        else:
            raise ValueError("Invalid plot type in config file")

    def compute_time_series(self):
        """Aggregates data entries across models for time series visualization."""
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                data_entries.append(self.multi_model.models[j].processed_host_data[i])
            self.meta_model.processed_host_data.append(self.meta_simulation_function(data_entries))

    def plot_time_series(self):
        """Plots time series data by appending metamodel to the models list and generating a plot."""
        self.multi_model.models.append(self.meta_model)
        self.multi_model.generate_plot()

    def compute_cumulative(self):
        """Aggregates cumulative data entries across models."""
        for i in range(0, self.min_raw_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                host_data = self.multi_model.models[j].raw_host_data
                ith_element = host_data[i]
                data_entries.append(ith_element)
            self.meta_model.cumulated += self.mean(data_entries)
        self.meta_model.cumulated = round(self.meta_model.cumulated, 2)

    def plot_cumulative(self):
        """Plots cumulative data by appending metamodel to the models list and generating a plot."""
        self.multi_model.models.append(self.meta_model)
        self.multi_model.generate_plot()

    def compute_cumulative_time_series(self):
        """Aggregates data entries across models for cumulative time series visualization."""
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                data_entries.append(self.multi_model.models[j].processed_host_data[i])
            self.meta_model.processed_host_data.append(self.meta_simulation_function(data_entries))

    def plot_cumulative_time_series(self):
        """Plots cumulative time series data by appending metamodel to the models list and generating a plot."""
        self.multi_model.models.append(self.meta_model)
        self.multi_model.generate_plot()

    def output_metamodel(self):
        """Exports the processed host data of the metamodel to a parquet file."""
        directory_path = os.path.join(self.multi_model.output_folder_path, "raw-output/metamodel/seed=0")
        os.makedirs(directory_path, exist_ok=True)
        current_path = os.path.join(directory_path, f"{self.multi_model.metric}.parquet")
        df = pd.DataFrame({'processed_host_data': self.meta_model.processed_host_data})
        df.to_parquet(current_path, index=False)

    def mean(self, chunks):
        """Calculates the mean of the given data chunks."""
        return np.mean(chunks)

    def median(self, chunks):
        """Calculates the median of the given data chunks."""
        return np.median(chunks)

    def meta_equation1(self, chunks):
        """
        Calculates a weighted mean where weights are inversely proportional to the absolute difference from the median.

        Args:
            chunks (list): Data chunks from which to calculate the weighted mean.

        Returns:
            float: The calculated weighted mean.
        """
        median_val = np.median(chunks)
        proximity_weights = 1 / (1 + np.abs(chunks - median_val))  # Avoid division by zero
        weighted_mean = np.sum(proximity_weights * chunks) / np.sum(proximity_weights)
        return weighted_mean
