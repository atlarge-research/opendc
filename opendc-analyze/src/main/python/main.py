import sys

from experiments import experiment6
from input_parser import read_input


def main():
    experiment6.experiment6_fr6(
        user_input=read_input(sys.argv[2]),
        path=sys.argv[1],
    )


if __name__ == "__main__":
    main()
