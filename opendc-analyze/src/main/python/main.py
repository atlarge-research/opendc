import os
import time

import experiments
import utils
import sys
from models.MetaModel import Metamodel
from models.MultiModel import MultiModel
from input_parser import read_input
# from experiments import window_size_time_analysis
# from experiments import use_cases

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


# experiments/#1-more-windows-same-plot/outputs//window_size_comparison_same_plot

def main():
    # create a file and write in it argv[1]
    # use os to create a file
    os.makedirs("output", exist_ok=True)
    with open("output/output.txt", "w") as f:
        f.write("The first received argument is: " + sys.argv[1])
        f.write("\n")
        f.write("The second received argument is: " + sys.argv[2])





main()
