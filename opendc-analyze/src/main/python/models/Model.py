"""
A model is the output of simulator. It contains the data the simulator output, under a certain topology, seed,
workload, datacenter configuration, etc. A model is further used in the analyzer as part of the MultiModel class,
and further in the MetaModel class.

:param host: the host data of the model
"""
class Model:
    def __init__(self, raw_host_data, name="notfornow"):
        self.raw_host_data = raw_host_data
        self.processed_host_data = []
        self.name = name

