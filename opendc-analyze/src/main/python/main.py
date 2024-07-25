import sys

from experiments import experiment7, experiment8, experiment5
from input_parser import read_input
from models.MultiModel import MultiModel


# def main():
#     experiment8.experiment_8(
#         user_input=read_input(sys.argv[2]),
#         path=sys.argv[1],
#     )

def main():
    experiment5.experiment_5(
        MultiModel(
            user_input=read_input(sys.argv[2]),
            path=sys.argv[1],
        )
    )

if __name__ == "__main__":
    main()
