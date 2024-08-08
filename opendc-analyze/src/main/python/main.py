from os import sys

from input_parser import read_input
from models.MetaModel import MetaModel
from models.MultiModel import MultiModel


def main():
    multimodel = MultiModel(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )

    multimodel.generate_plot()

    MetaModel(multimodel)


if __name__ == "__main__":
    main()
