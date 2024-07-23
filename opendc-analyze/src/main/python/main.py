import os
import utils
import sys
import time
import pandas as pd

from experiments import experiment0, experiment1, experiment2, experiment3, experiment4, experiment5, experiment6

from models.MultiModel import MultiModel
from models.MetaModel import MetaModel
from input_parser import read_input


def main():
    experiment6.experiment_6(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )


if __name__ == "__main__":
    main()
