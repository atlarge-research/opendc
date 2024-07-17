import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

import utils
from .Model import Model

META_MODEL_ID = 101


class Metamodel:
    def __init__(self, multimodel):
        if multimodel.user_input['metamodel']:
            self.multimodel = multimodel
            self.meta_model_entries = []
            self.meta_model_function = multimodel.user_input['meta_simulation_function']
            self.meta_cumulative = 0
            self.cumulative_time_series = 0
            self.compute_metamodel()
        else:
            raise ValueError("Metamodel is not enabled in the config file")

    def compute_metamodel(self):
        min_processed_model_len = min([len(model.processed_host_data) for model in self.multimodel.models])
        min_raw_model_len = min([len(model.raw_host_data) for model in self.multimodel.models])
        number_of_models = len(self.multimodel.models)

        if self.multimodel.user_input['plot_type'] == 'cumulative':
            for i in range(0, min_raw_model_len):
                data_entries = []
                for j in range(number_of_models):
                    host_data = self.multimodel.models[j].raw_host_data
                    ith_element = host_data.iloc[i]
                    data_entries.append(ith_element)
                self.meta_cumulative += self.mean(data_entries)
        elif self.multimodel.user_input['plot_type'] == 'time_series':
            if self.multimodel.window_size == 1:
                self.compute_for_window_1(min_processed_model_len, number_of_models)
            else:
                self.compute_for_any_window(min_processed_model_len, number_of_models)

        elif self.multimodel.user_input['plot_type'] == 'cumulative_time_series':
            pass

    def compute_for_window_1(self, min_processed_model_len, number_of_models):
        for i in range(0, min_processed_model_len):
            data_entries = []
            for j in range(number_of_models):
                host_data = self.multimodel.models[j].raw_host_data
                ith_element = host_data.iloc[i]
                data_entries.append(ith_element)
            self.meta_model_entries.append(self.mean(data_entries))

    def compute_for_any_window(self, min_processed_model_len, number_of_models):
        for i in range(0, min_processed_model_len):
            data_entries = []
            for j in range(number_of_models):
                host_data = self.multimodel.models[j].processed_host_data
                ith_element = host_data[i]
                data_entries.append(ith_element)

            self.meta_model_entries.append(self.mean(data_entries))

    def mean(self, chunks):
        return np.mean(chunks)

    def append_to_multi_model(self):
        meta_model = Model(
            raw_host_data=pd.Series(self.meta_model_entries),
            id=META_MODEL_ID,
            path=self.multimodel.output_folder_path
        )

        if self.multimodel.user_input['plot_type'] == 'cumulative':
            meta_model.cumulated = round(self.meta_cumulative, 2)

        meta_model.processed_host_data = pd.Series(self.meta_model_entries)
        self.multimodel.models.append(meta_model)

    def plot_metamodel(self):
        self.append_to_multi_model()
        self.multimodel.generate_plot()
