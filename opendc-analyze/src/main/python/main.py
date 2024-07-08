import os
import time

import experiments
import utils
import sys
from models.MultiModel import MultiModel
from input_parser import read_input  # Corrected import


# e.g., argument 1 -- experiments/experiment-2-window-performance-analysis/inputs/analyzer.json
# e.g., argument 2 -- experiments/experiment-2-window-performance-analysis/outputs//window_size_comparison_same_plot

def main():
    print("ARGV1 is " + sys.argv[1])
    print("ARGV2 is " + sys.argv[2])
    # create a file and write in it argv[1]
    # use os to create a file
    print("The current path is " + os.getcwd())
    user_input = read_input(sys.argv[1])

    multimodel = MultiModel(
        metric=user_input["metric"],
        window_size=user_input["window_size"],
        aggregation_function=user_input["aggregation_function"],
        plot_type=user_input["plot_type"],
        path=sys.argv[2]
    )

    multimodel.generate_plot()


if __name__ == "__main__":
    main()
