import numpy as np
from scipy.stats import mstats

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

    def compute(self):
        if self.multimodel.user_input['plot_type'] == 'time_series':
            self.compute_time_series()

        elif self.multimodel.user_input['plot_type'] == 'cumulative':
            self.compute_cumulative()

        elif self.multimodel.user_input['plot_type'] == 'cumulative_time_series':
            self.compute_cumulative_time_series()

    def plot(self):
        if self.multimodel.user_input['plot_type'] == 'time_series':
            self.plot_time_series()

        elif self.multimodel.user_input['plot_type'] == 'cumulative':
            self.plot_cumulative()

        elif self.multimodel.user_input['plot_type'] == 'cumulative_time_series':
            self.plot_cumulative_time_series()

    def compute_time_series(self):
        for i in range(0, self.min_raw_model_len):
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

    def mean(self, chunks):
        return np.mean(chunks)

    def median(self, chunks):
        return np.median(chunks)


    def meta_equation1(self, chunks):
        median_val = np.median(chunks)
        proximity_weights = 1 / (1 + np.abs(chunks - median_val))  # Avoid division by zero
        weighted_mean = np.sum(proximity_weights * chunks) / np.sum(proximity_weights)
        return weighted_mean


    ####IN "BETA"####
    #
    # def meta_equation2(self, chunks, trim_percentage=10):
    #     sorted_chunks = np.sort(chunks)
    #     n = len(chunks)
    #     trim_count = int(trim_percentage / 100 * n)
    #     trimmed_chunks = sorted_chunks[trim_count:-trim_count]  # Remove trim_count elements from both ends
    #     return np.mean(trimmed_chunks)
    #
    # def meta_equation3(self, chunks, limits=0.1):
    #     winsorized_chunks = mstats.winsorize(chunks, limits=[limits, limits])
    #     return np.mean(winsorized_chunks)
