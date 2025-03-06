"""
A model is the output of simulator. It contains the data the simulator output, under a certain topology, seed,
workload, datacenter configuration, etc. A model is further used in the analyzer as part of the MultiModel class,
and further in the MetaModel class.

:param sim: the simulation data of the model
"""
import json
from dataclasses import dataclass, field

@dataclass
class Model:
    """
    Represents a single simulation output containing various data metrics collected under specific simulation conditions.
    A Model object stores raw and processed simulation data and is designed to interact with higher-level structures like
    MultiModel and MetaModel for complex data analysis.

    Attributes:
        raw_sim_data (list): Initial raw data from the simulator output.
        processed_sim_data (list): Data derived from raw_sim_data after applying certain processing operations like aggregation or smoothing.
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

    path: str
    raw_sim_data: list
    id: int
    processed_sim_data: list = field(default_factory=list)
    cumulative_time_series_values: list = field(default_factory=list)
    cumulated: float = 0.0
    experiment_name: str = ""
    margins_of_error: list = field(default_factory=list)
    topologies: list = field(default_factory=list)
    workloads: list = field(default_factory=list)
    allocation_policies: list = field(default_factory=list)
    carbon_trace_paths: list = field(default_factory=list)

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
            self.experiment_name = trackr.get(self.id, {}).get('name', "")
            self.topologies = trackr.get(self.id, {}).get('topologies', [])
            self.workloads = trackr.get(self.id, {}).get('workloads', [])
            self.allocation_policies = trackr.get(self.id, {}).get('allocationPolicies', [])
            self.carbon_trace_paths = trackr.get(self.id, {}).get('carbonTracePaths', [])
