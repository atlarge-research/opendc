import os
import time

from experiments.experiment2 import experiment_2
import utils
import sys
# from models.MetaModel import Metamodel
from models.MultiModel import MultiModel
from input_parser import read_input


# e.g., argument 1 -- experiments/experiment-2-window-performance-analysis/inputs/analyzer.json
# e.g., argument 2 -- experiments/experiment-2-window-performance-analysis/outputs//window_size_performance_analysis

# parameter input for main
# "experiments/experiment-2-window-performance-analysis/inputs/analyzer.json", "experiments/experiment-2-window-performance-analysis/outputs/window-size-performance-analysis"

def main():
    experiment_2()
    # print("ARGV1 is " + sys.argv[1])
    # print("ARGV2 is " + sys.argv[2])
    # print("The current path is " + os.getcwd())
    # user_input = read_input(sys.argv[1])
    #
    # # if user wants to run a multi-model
    # if (user_input["multimodel"]):
    #     multimodel = MultiModel(
    #         user_input,
    #         path=sys.argv[2]
    #     )
    #     multimodel.generate_plot()



if __name__ == "__main__":
    main()
