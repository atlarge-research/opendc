import os
import utils
import sys
import time
import pandas as pd

from experiments import experiment0
from experiments import experiment1
from experiments import experiment2
from experiments import experiment3
from experiments import experiment4
from experiments import experiment5

from models.MultiModel import MultiModel
from models.MetaModel import MetaModel
from input_parser import read_input


def main():
    experiment5.experiment_5(
        MultiModel(
            user_input=read_input(sys.argv[2]),
            path=sys.argv[1],
        )
    )


if __name__ == "__main__":
    main()
