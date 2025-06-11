"""
A model is the output of simulator. It contains the data the simulator output, under a certain topology, seed,
workload, datacenter configuration, etc. A model is further used in the analyzer as part of the MultiModel class,
and further in the MetaModel class.

:param sim: the simulation data of the model
"""
import json


class Model:
    """
    Represents a single simulation output containing various data metrics collected under specific simulation conditions.
    A Model object stores raw and processed simulation data and is designed to interact with higher-level structures like
    MultiModel and MetaModel for complex data analysis.
    """

    def __init__(self, raw_sim_data, identifier: str):
        self.raw_sim_data = raw_sim_data
        self.id: str = str(identifier)
        self.processed_sim_data = []
        self.cumulative_time_series_values = []
        self.cumulated: float = 0.0
        self.experiment_name: str = ""
        self.margins_of_error = []
        self.topologies = []
        self.workloads = []
        self.allocation_policies = []
        self.carbon_trace_paths = []

    def is_meta_model(self) -> bool:
        return self.id == "M"
