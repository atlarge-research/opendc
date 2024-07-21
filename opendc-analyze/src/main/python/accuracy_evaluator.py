import numpy as np


def accuracy_evaluator(
    real_data=None,
    multi_model=None,
    compute_mape=True,
    compute_nad=True,
    compute_rmsle=True,
    rmsle_hyperparameter=0.5
):
    """
    :param real_data: the real-world data of the simulation
    :param multi_model: the Multi-Model, containing individual models (possibly also a Meta-Model, with id=101)
    :param MAPE: whether to calculate Mean Absolute Percentage Error (MAPE)
    :param NAD: whether to calculate Normalized Absolute Differences (NAD)
    :param RMSLE: whether to calculate Root Mean Square Logarithmic Error (RMSLE)
    :param rmsle_hyperparameter: the hyperparameter that balances the ration underestimations:overestimations
        - default is 0.5 (balanced penalty)
        - < 0.5: more penalty for overestimations
        - > 0.5: more penalty for underestimations
        e.g., RMSLE_hyperparameter=0.3 -> 30% penalty for overestimations, 70% penalty for underestimations (3:7 ratio)
    :return: None, but prints the accuracy metrics
    """

    for model in multi_model.models:
        simulation_data = model.raw_host_data
        min_len = min(len(real_data), len(simulation_data))
        real_data = real_data[:min_len]
        simulation_data = simulation_data[:min_len]
        if compute_mape:
            accuracy_mape = mape(
                real_data=real_data,
                simulation_data=simulation_data
            )
            print("Mean Absolute Percentage Error (MAPE): ", accuracy_mape)

        if compute_nad:
            accuracy_nad = nad(
                real_data=real_data,
                simulation_data=simulation_data
            )
            print("Normalized Absolute Differences (NAD): ", accuracy_nad)

        if compute_rmsle:
            accuracy_rmsle = rmsle(
                real_data=real_data,
                simulation_data=simulation_data,
                alpha=rmsle_hyperparameter
            )
            print("Root Mean Square Logarithmic Error (RMSLE), alpha=", rmsle_hyperparameter, ": ", accuracy_rmsle)


def mape(real_data, simulation_data):
    """
    Calculate Mean Absolute Percentage Error (MAPE)
    :param real_data: Array of real values
    :param simulation_data: Array of simulated values
    :return: MAPE value
    """
    real_data = np.array(real_data)
    simulation_data = np.array(simulation_data)
    return np.mean(np.abs((real_data - simulation_data) / real_data)) * 100


def nad(real_data, simulation_data):
    """
    Calculate Normalized Absolute Differences (NAD)
    :param real_data: Array of real values
    :param simulation_data: Array of simulated values
    :return: NAD value
    """
    real_data = np.array(real_data)
    simulation_data = np.array(simulation_data)
    return np.sum(np.abs(real_data - simulation_data)) / np.sum(real_data) * 100


def rmsle(real_data, simulation_data, alpha=0.5):
    """
    Calculate Root Mean Square Logarithmic Error (RMSLE) with an adjustable alpha parameter
    :param real_data: Array of real values
    :param simulation_data: Array of simulated values
    :param alpha: Hyperparameter that balances the penalty between underestimations and overestimations
    :return: RMSLE value
    """
    real_data = np.array(real_data)
    simulation_data = np.array(simulation_data)
    log_diff = alpha * np.log(real_data) - (1 - alpha) * np.log(simulation_data)
    return np.sqrt(np.mean(log_diff ** 2))
