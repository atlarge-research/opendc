from input_parser import read_input
from models.MultiModel import MultiModel

"""
Goal: plot 3 plots, with 3 different window sizes.

Experiment 3 plots as the user inputs in the file m3saSetup.json, located in the inputs folder. This function only
creates a Multi-Model and generates a plot for the user's input.
"""


def experiment_3(argv1, argv2):
    user_input = read_input(argv2)

    multimodel = MultiModel(
        user_input,
        path=argv1
    )
    multimodel.generate_plot()
