import os
import time

import experiments
import utils
from models.MetaModel import Metamodel
from models.MultiModel import MultiModel


def main():
    os.chdir(utils.SIMULATION_FOLDER_PATH)
    experiments.exp1_window_sizes(window_sizes=[10, 100, 1000, 5000])

    Metamodel(multimodel=MultiModel(input_metric="carbon_emission", window_size=100)).generate()
    Metamodel(multimodel=MultiModel(input_metric="power_draw", window_size=100)).generate()


main()
