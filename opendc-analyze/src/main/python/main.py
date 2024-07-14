import os
import utils
import sys
import time

from experiments import experiment0
from experiments import experiment1
from experiments import experiment2
from experiments import experiment3

from models.MultiModel import MultiModel
# from models.MetaModel import Metamodel
from input_parser import read_input


def main():
    experiment2.experiment_2()

    # experiment3.experiment_3(
    #     argv1=sys.argv[1],
    #     argv2=sys.argv[2]
    # )




if __name__ == "__main__":
    main()
