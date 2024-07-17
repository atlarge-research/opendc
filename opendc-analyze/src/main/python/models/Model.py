"""
A model is the output of simulator. It contains the data the simulator output, under a certain topology, seed,
workload, datacenter configuration, etc. A model is further used in the analyzer as part of the MultiModel class,
and further in the MetaModel class.

:param host: the host data of the model
"""
import json
import os


class Model:
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
        trackr_path = self.path + "/trackr.json"
        with open(trackr_path) as f:
            trackr = json.load(f)
            self.experiment_name = trackr[self.id]['name']
            self.topologies = trackr[self.id]['topologies']
            self.workloads = trackr[self.id]['workloads']
            self.allocation_policies = trackr[self.id]['allocationPolicies']
            self.carbon_trace_paths = trackr[self.id]['carbonTracePaths']
