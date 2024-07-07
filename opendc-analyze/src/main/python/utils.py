import json
import os
import sys

import matplotlib.pyplot as plt

"""
Constants for the main.py file
"""

SIMULATION_FOLDER_PATH = './../../../experiments/' + sys.argv[1]
RAW_OUTPUT_FOLDER_PATH = SIMULATION_FOLDER_PATH + '/raw-output/'
ANALYSIS_FOLDER_PATH = SIMULATION_FOLDER_PATH + '/simulation-analysis/'

EMISSIONS_ANALYSIS_FOLDER_PATH = ANALYSIS_FOLDER_PATH + 'carbon_emission/'
ENERGY_ANALYSIS_FOLDER_PATH = ANALYSIS_FOLDER_PATH + 'power_draw/'

SIMULATION_ANALYSIS_FOLDER_NAME = 'simulation-analysis'
EMISSIONS_ANALYSIS_FOLDER_NAME = 'carbon_emission'
ENERGY_ANALYSIS_FOLDER_NAME = 'power_draw'


"""
Utility functions
"""
def clean_analysis_file(metric):
    analysis_file_path = SIMULATION_ANALYSIS_FOLDER_NAME + "/"
    if metric == "power_draw":
        analysis_file_path += ENERGY_ANALYSIS_FOLDER_NAME
    else:
        analysis_file_path += EMISSIONS_ANALYSIS_FOLDER_NAME
    analysis_file_path += "/analysis.txt"

    with open(analysis_file_path, "w") as f:
        f.write("")



