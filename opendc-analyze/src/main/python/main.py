import sys

from experiments import experiment7
from input_parser import read_input


def main():
    experiment7.experiment_7(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )


if __name__ == "__main__":
    main()
