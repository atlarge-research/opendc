"""
A model is the output of simulator. It contains the data the simulator output, under a certain topology, seed,
workload, datacenter configuration, etc. A model is further used in the analyzer as part of the MultiModel class,
and further in the MetaModel class.

:param host: the host data of the model
"""
import json


class Model:
    def __init__(self, raw_host_data, id):
        self.raw_host_data = raw_host_data
        self.processed_host_data = []
        self.experiment_name = id
        self.id = id

        self.margins_of_error = []
        self.topologies = []
        self.workloads = []
        self.allocationPolicies = []
        self.carbonTracePaths = []

        self.parse_trackr()


    def parse_trackr(self):
        # open trackr.json, located in the same folder as we are now
        with open("trackr.json") as f:
            trackr = json.load(f)
            self.experiment_name = trackr[self.id]['name']
            self.topologies = trackr[self.id]['topologies']
            self.workloads = trackr[self.id]['workloads']
            self.allocationPolicies = trackr[self.id]['allocationPolicies']
            self.carbonTracePaths = trackr[self.id]['carbonTracePaths']
