import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

import utils
from .Model import Model


class Metamodel:
    def __init__(self, multimodel):
        if multimodel.user_input['metamodel']:
            self.multimodel = multimodel
            self.meta_model_entries = []
            self.meta_model_function = multimodel.user_input['meta_simulation_function']
            self.compute_metamodel()
        else:
            raise ValueError("Metamodel is not enabled in the config file")

    def compute_metamodel(self):
        min_model_len = min([len(model.processed_host_data) for model in self.multimodel.models])
        number_of_models = len(self.multimodel.models)

        if self.multimodel.window_size == 1:
            self.compute_metamodel_single_window(max_length, number_of_models)
        else:
            for i in range(0, min_model_len):
                data_entries = []
                for j in range(number_of_models):
                    data_entries.append(self.multimodel.models[j].processed_host_data[i]) # iloc is the pandas equivalent of .at()

                self.meta_model_entries.append(self.mean(data_entries))

    def compute_metamodel_single_window(self, max_length, number_of_models):
        for i in range(0, max_length):
            data_entries = []
            for j in range(number_of_models):
                data_entries.append(self.multimodel.models[j].processed_host_data.iloc[i])

            self.meta_model_entries.append(self.mean(data_entries))

    def mean(self, chunks):
        return np.mean(chunks)

    def append_to_multi_model(self):
        meta_model = Model(
            raw_host_data=self.meta_model_entries,
            id=101,
            path=self.multimodel.output_folder_path
        )

        size_of_meta_model = len(self.meta_model_entries)
        size_of_model = len(self.multimodel.models[0].processed_host_data)
        meta_model.processed_host_data = self.meta_model_entries
        self.multimodel.models.append(meta_model)

    def plot_metamodel(self):
        self.append_to_multi_model()
        self.multimodel.generate_plot()
