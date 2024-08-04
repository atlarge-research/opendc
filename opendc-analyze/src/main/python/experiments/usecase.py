from models.MetaModel import MetaModel
from models.MultiModel import MultiModel

"""
Goal: analyze the accuracy, per model, using MAPE, NAD, RMSLE.

Experiment 5 computes a multi-model with various models, then a meta-model. It then feeds in the accuracy evaluator
and generates a peformance report, per model.
"""


def usecase(user_input, path):
    """
    GOAL: Analyze the accuracy of each individual model, and output in a file.
    Then, analyze the accuracy of Meta-Model using mean, median, and the meta-equation.
    Analyze against ground truth, located in inputs/surf-sara/trace/ground_truth.parquet.
    """
    multimodel = MultiModel(
        user_input=user_input,
        path=path
    )

    multimodel.generate_plot()

    MetaModel(multimodel)
