import os
import time

import experiments
import utils
from models.MetaModel import Metamodel
from models.MultiModel import MultiModel

# an example of program arguments is output/4-models-and-ground-truth
# similar to $folder_name$/$subfolder_name$


def main():
    os.chdir(utils.SIMULATION_FOLDER_PATH)
    experiments.exp1_window_sizes(window_sizes=[2, 20, 200, 2000])

    Metamodel(multimodel=MultiModel(input_metric="carbon_emission", window_size=100)).generate()
    Metamodel(multimodel=MultiModel(input_metric="power_draw", window_size=100)).generate()


main()
