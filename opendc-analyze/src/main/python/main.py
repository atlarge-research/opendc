import os
import utils
import sys
import time

from experiments import experiment0
from experiments import experiment1
from experiments import experiment2

from models.MultiModel import MultiModel
# from models.MetaModel import Metamodel
from input_parser import read_input


# e.g., argument 1 -- experiments/experiment-2-window-performance-analysis/outputs//window_size_performance_analysis
# e.g., argument 2 -- experiments/experiment-2-window-performance-analysis/inputs/analyzer.json

# "experiments/testing-multimodel/outputs//testing"
# "experiments/testing-multimodel/inputs/analyzer.json"

# demo_alex/outputs/

# parameter input for main
# "experiments/experiment-2-window-performance-analysis/inputs/analyzer.json", "experiments/experiment-2-window-performance-analysis/outputs/window-size-performance-analysis"

def main():
    print("ARGV1 is " + sys.argv[1])
    print("ARGV2 is " + sys.argv[2])

    # argv for Alexandru's demo "demo_alex/outputs/" "demo_alex/inputs/analyzer.json"
    print("The current path is " + os.getcwd())



    user_input = read_input(sys.argv[2])

    if (user_input["multimodel"]):  # if user wants to run a Multi-Model
        multimodel = MultiModel(
            user_input,
            path=sys.argv[1]
        )
        multimodel.generate_plot()


if __name__ == "__main__":
    main()
