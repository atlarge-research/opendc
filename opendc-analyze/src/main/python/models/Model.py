"""
A model is the output of simulator. It contains the data the simulator output, under a certain topology, seed,
workload, datacenter configuration, etc. A model is further used in the analyzer as part of the MultiModel class,
and further in the MetaModel class.

:param host: the host data of the model
"""
import json
import os


class Model:
    """
    Represents a single simulation output containing various data metrics collected under specific simulation conditions.
    A Model object stores raw and processed simulation data and is designed to interact with higher-level structures like
    MultiModel and MetaModel for complex data analysis.

    Attributes:
        raw_host_data (list): Initial raw data from the simulator output, specific to a given host in the simulation.
        processed_host_data (list): Data derived from raw_host_data after applying certain processing operations like aggregation or smoothing.
        cumulative_time_series_values (list): Stores cumulative data values useful for time series analysis.
        id (int): Unique identifier for the model, typically used for tracking and referencing within analysis tools.
        path (str): Base path for storing or accessing related data files.
        cumulated (float): Cumulative sum of processed data, useful for quick summaries and statistical analysis.
        experiment_name (str): A descriptive name for the experiment associated with this model, potentially extracted from external metadata.
        margins_of_error (list): Stores error margins associated with the data, useful for uncertainty analysis.
        topologies (list): Describes the network or system topologies used during the simulation.
        workloads (list): Lists the types of workloads applied during the simulation, affecting the simulation's applicability and scope.
        allocation_policies (list): Details the resource allocation policies used, which influence the simulation outcomes.
        carbon_trace_paths (list): Paths to data files containing carbon output or usage data, important for environmental impact studies.

    Methods:
        parse_trackr(): Reads additional configuration and metadata from a JSON file named 'trackr.json', enhancing the model with detailed context information.

    Usage:
        Model objects are typically instantiated with raw data from simulation outputs and an identifier. After instantiation,
        the 'parse_trackr' method can be called to load additional experimental details from a corresponding JSON file.
    """
    def __init__(self, raw_host_data, id, path):
        self.path = path
        self.raw_host_data = raw_host_data
        self.processed_host_data = []
        self.cumulative_time_series_values = []
        self.experiment_name = id
        self.id = id
        self.cumulated = 0

        self.margins_of_error = []
        self.topologies = []
        self.workloads = []
        self.allocation_policies = []
        self.carbon_trace_paths = []

        # self.parse_trackr()

    def parse_trackr(self):
        """
        Parses the 'trackr.json' file located in the model's base path to extract and store detailed experimental metadata.
        This method enhances the model with comprehensive contextual information about the simulation environment.

        :return: None
        :side effect: Updates model attributes with data from the 'trackr.json' file, such as experiment names, topologies, and policies.
        :raises FileNotFoundError: If the 'trackr.json' file does not exist at the specified path.
        :raises json.JSONDecodeError: If there is an error parsing the JSON data.
        """
        trackr_path = self.path + "/trackr.json"
        with open(trackr_path) as f:
            trackr = json.load(f)
            self.experiment_name = trackr[self.id]['name']
            self.topologies = trackr[self.id]['topologies']
            self.workloads = trackr[self.id]['workloads']
            self.allocation_policies = trackr[self.id]['allocationPolicies']
            self.carbon_trace_paths = trackr[self.id]['carbonTracePaths']
