import os

import matplotlib.pyplot as plt
import numpy as np

import utils
import time

from models.MultiModel import MultiModel

"""
Experiment goal: determine the difference in energy usage between different simulation models.

This experiment is part of RQ1 and runs the first use case. We hypothetically provide a large-scale datacenter, with
a scenario in which its scaling and maintenance team wants to understand the behaviour of the infrastructure.

This experiment simulates the energy usage of four different simulation models. Then, it plots, in a horizontal bar
chart, the total cumulated energy, as of model 1. Further, in a different plot, it plots the cumulated energy, predicted by
all 4 models.

All the relevant information is saved in use_case_analysis.txt
"""


def rq1_case_1():
    _input_metric = "power_draw"
    _window_size = 100
    multimodel = MultiModel(
        input_metric=_input_metric,
        window_size=_window_size
    )
    number_of_samples = len(multimodel.models[0].raw_host_data)
    cumulated_energies = np.array(multimodel.get_cumulated()) / 1000  # convert to MWh
    # round all the values in cumulated_energies to 3 decimal places
    cumulated_energies = np.round(cumulated_energies, 3)

    plot_horizontal_bar_chart(cumulated_energies, number_of_models=1)
    plot_horizontal_bar_chart(cumulated_energies, number_of_models=4)
    output_simulation_details(multimodel, _input_metric, _window_size, number_of_samples, cumulated_energies)


def output_simulation_details(multimodel, input_metric, window_size, number_of_samples, cumulated_energies):
    with open('use_case_analysis.txt', 'a') as f:
        # Write to the file here
        average_cpu_utilizations = multimodel.get_average_cpu_utilization()
        f.write("\n\n========================================\n")
        f.write("Simulation made at " + time.strftime("%Y-%m-%d %H:%M:%S") + "\n")
        f.write(
            "We are running use_cases.rq1_case_1() for " + input_metric + ", on a multimodel with window size " + str(
                window_size) + "\n")
        f.write("Sample count in raw host data: " + str(number_of_samples) + "\n")
        for (i, cumulated_energy) in enumerate(cumulated_energies):
            f.write("Average CPU utilization for model " + str(i) + ": " + str(average_cpu_utilizations[i]) + "\n")
            f.write("Cumulated energy usage for model " + str(i) + ": " + str(cumulated_energy) + " MWh\n")


def plot_horizontal_bar_chart(cumulated_energies, number_of_models):
    plt.figure(figsize=(10, 10))
    plt.title("Cumulated energy usage for " + str(number_of_models) + " model(s)")
    plt.ylabel("Model ID")
    plt.xlabel("Cumulated energy usage")
    plt.yticks(range(1, number_of_models))
    colors = ['#002B7F', '#FF0000', '#FFEC00', "#00FF00"]

    if number_of_models != len(cumulated_energies):
        for i in range(0, number_of_models):
            plt.barh(i, cumulated_energies[i], color=colors[i])
    else:
        max_energy = max(cumulated_energies)
        min_energy = min(cumulated_energies)
        standard_deviation = np.std(cumulated_energies)
        plt.xlim(min_energy - standard_deviation, max_energy + standard_deviation)

        for i in range(0, len(cumulated_energies)):
            plt.barh(i, cumulated_energies[i], color=colors[i])


    plt.savefig(
        "./" + utils.SIMULATION_ANALYSIS_FOLDER_NAME + "/cumulated_energy_usage_" + str(number_of_models) + "_models.png")


