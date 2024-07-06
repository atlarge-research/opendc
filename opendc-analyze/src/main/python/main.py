import os
import time

import experiments
import utils
from models.MetaModel import Metamodel
from models.MultiModel import MultiModel
from experiments import window_size_time_analysis
from experiments import use_cases

# an example of program arguments is output/4-models-and-ground-truth
# similar to $folder_name$/$subfolder_name$

"""
User input data
"""

# Multimodel
# input_metric = "power_draw" or "carbon_emission"
# window_size
# aggregation_function
# plot_type = "cummulative" or "line"
# x_label
# y_label
# plot_title

def main():
    # output/surf-sara-sim
    os.chdir(utils.SIMULATION_FOLDER_PATH)
    window_size_time_analysis.experiment_1()


main()
