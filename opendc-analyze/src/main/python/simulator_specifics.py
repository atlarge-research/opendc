"""
This file is the integration layer of the Multi-Meta-Model tool upon any (ICT) simulator.

The system will use the elements from this file in the analysis / meta-simulation process.
"""

"""
SIMULATION_DATA_FILE (str): The name of the file containing the simulation data. Enter only the name, not the path, not
the extension. The data file must be parquet format.

✅ Good: "host", "simulation_data", "cats_predictions"
❌ Wrong: "host.json", "opendc/folder_x/folder_y/data"
"""
SIMULATION_DATA_FILE = "host"  # opendc outputs in file host.parquet
