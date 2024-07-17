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
            self.metamodel = Model(
                raw_host_data=pd.Series(),
                id=META_MODEL_ID,
                path=self.multimodel.output_folder_path
            )


            self.meta_model_entries = []
            self.meta_model_function = multimodel.user_input['meta_simulation_function']
            self.min_raw_model_len = min([len(model.raw_host_data) for model in self.multimodel.models])
            self.min_processed_model_len = min([len(model.processed_host_data) for model in self.multimodel.models])
            self.number_of_models = len(self.multimodel.models)
            self.meta_cumulative = 0
            self.compute()

        else:
            raise ValueError("Metamodel is not enabled in the config file")

    def compute(self):
        if self.multimodel.user_input['plot_type'] == 'time_series':
            self.compute_time_series()

        elif self.multimodel.user_input['plot_type'] == 'cumulative':
            self.compute_cumulative()

        elif self.multimodel.user_input['plot_type'] == 'cumulative_time_series':
            self.compute_cumulative_time_series()


    def compute_cumulative(self):
        for i in range(0, self.min_raw_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                host_data = self.multimodel.models[j].raw_host_data
                ith_element = host_data.iloc[i]
                data_entries.append(ith_element)
            self.meta_cumulative += self.mean(data_entries)

    def compute_time_series(self):
        if self.multimodel.window_size == 1:
            self.compute_for_window_1()
        else:
            self.compute_for_any_window()

    def compute_cumulative_time_series(self):
        sum = 0
        self.meta_model_entries.append(sum)
        for i in range(0, self.min_raw_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                host_data = self.multimodel.models[j].raw_host_data
                ith_element = host_data.iloc[i]
                data_entries.append(ith_element)
            sum += self.mean(data_entries)
            self.meta_model_entries.append(sum)

    def compute_for_window_1(self):
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for j in range(self.number_of_models):
                host_data = self.multimodel.models[j].raw_host_data
                ith_element = host_data.iloc[i]
                data_entries.append(ith_element)
            self.meta_model_entries.append(self.mean(data_entries))

    def compute_for_any_window(self):
        for i in range(0, self.min_processed_model_len):
            data_entries = []
            for j in range(self.number_of_models):
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
        if self.multimodel.plot_type == 'cumulative_time_series':
            self.plot_cumulative_time_series()
        else:
            self.append_to_multi_model()
            self.multimodel.generate_plot()


    def plot_cumulative_time_series(self):
        meta_model = Model(
            raw_host_data=pd.Series(self.meta_model_entries),
            id=META_MODEL_ID,
            path=self.multimodel.output_folder_path
        )

        self.multimodel.models.append(meta_model)
        self.append_to_multi_model()
        self.multimodel.generate_plot()
