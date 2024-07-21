import numpy as np
import pandas as pd
import os

import utils
from .Model import Model

META_MODEL_ID = 101


class Metamodel:
    def __init__(self, multimodel):
        if not multimodel.user_input.get('metamodel', False):
            raise ValueError("Metamodel is not enabled in the config file")

        self.function_map = {
            'mean': self.mean,
            'median': self.median,
            'equation1': self.meta_equation1,
        }

        self.multimodel = multimodel
        self.metamodel = Model(
            raw_host_data=[],
            id=META_MODEL_ID,
            path=self.multimodel.output_folder_path
        )

        self.meta_simulation_function = self.function_map.get(multimodel.user_input['meta_simulation_function'], self.mean)
        self.min_raw_model_len = min([len(model.raw_host_data) for model in self.multimodel.models])
        self.min_processed_model_len = min([len(model.processed_host_data) for model in self.multimodel.models])
        self.number_of_models = len(self.multimodel.models)
        self.compute()

    def output(self):
        self.plot()
        self.output_metamodel()

    def compute(self):
        if self.multimodel.user_input['plot_type'] == 'time_series':
            self.compute_time_series()
        elif self.multimodel.user_input['plot_type'] == 'cumulative':
            self.compute_cumulative()
        elif self.multimodel.user_input['plot_type'] == 'cumulative_time_series':
            self.compute_cumulative_time_series()
        else:
            raise ValueError("Invalid plot type in config file")

    def plot(self):
        if self.multimodel.user_input['plot_type'] == 'time_series':
            self.plot_time_series()
        elif self.multimodel.user_input['plot_type'] == 'cumulative':
            self.plot_cumulative()
        elif self.multimodel.user_input['plot_type'] == 'cumulative_time_series':
            self.plot_cumulative_time_series()

        else:
            raise ValueError("Invalid plot type in config file")

    def compute_time_series(self):
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                data_entries.append(self.multimodel.models[j].processed_host_data[i])
            self.metamodel.processed_host_data.append(self.meta_simulation_function(data_entries))

    def plot_time_series(self):
        self.multimodel.models.append(self.metamodel)
        self.multimodel.generate_plot()

    def compute_cumulative(self):
        for i in range(0, self.min_raw_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                host_data = self.multimodel.models[j].raw_host_data
                ith_element = host_data[i]
                data_entries.append(ith_element)
            self.metamodel.cumulated += self.mean(data_entries)
        self.metamodel.cumulated = round(self.metamodel.cumulated, 2)

    def plot_cumulative(self):
        self.multimodel.models.append(self.metamodel)
        self.multimodel.generate_plot()

    def compute_cumulative_time_series(self):
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                data_entries.append(self.multimodel.models[j].processed_host_data[i])
            self.metamodel.processed_host_data.append(self.meta_simulation_function(data_entries))

    def plot_cumulative_time_series(self):
        self.multimodel.models.append(self.metamodel)
        self.multimodel.generate_plot()

    """
    This function outputs the metamodel in a parquet file with a single column, containing the processed_host_data.
    The function outputs in a ```.parquet``` file format.
    """
    def output_metamodel(self):
        directory_path = os.path.join(self.multimodel.output_folder_path, "raw-output/metamodel/seed=0")
        os.makedirs(directory_path, exist_ok=True)
        current_path = os.path.join(directory_path, f"{self.multimodel.metric}.parquet")
        df = pd.DataFrame({'processed_host_data': self.metamodel.processed_host_data})
        df.to_parquet(current_path, index=False)


    def mean(self, chunks):
        return np.mean(chunks)

    def median(self, chunks):
        return np.median(chunks)


    def meta_equation1(self, chunks):
        median_val = np.median(chunks)
        proximity_weights = 1 / (1 + np.abs(chunks - median_val))  # Avoid division by zero
        weighted_mean = np.sum(proximity_weights * chunks) / np.sum(proximity_weights)
        return weighted_mean


