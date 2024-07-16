import os
import utils
import sys
import time

from experiments import experiment0
from experiments import experiment1
from experiments import experiment2
from experiments import experiment3
from experiments import experiment4

from models.MultiModel import MultiModel
from models.MetaModel import Metamodel
from input_parser import read_input


def main():
    multi_model = MultiModel(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )

    metaModel = Metamodel(multi_model)

    metaModel.plot_metamodel()



if __name__ == "__main__":
    main()
