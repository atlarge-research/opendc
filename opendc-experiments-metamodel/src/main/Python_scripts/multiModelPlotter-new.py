# %%
"""
Running this chunk of code is required for setting up imports and functionality
"""
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime
import os
from IPython.display import display, HTML

CHUNK_SIZE = 1000
BASE_FOLDER = "/opendc-experiments/opendc-experiments-metamodel/src/main"


def list_directories(directory):
    return [d for d in os.listdir(directory) if os.path.isdir(os.path.join(directory, d))]


def most_recent_directory(directory):
    dirs = list_directories(directory)
    if not dirs:
        return None
    most_recent = max(dirs, key=lambda d: os.path.getctime(os.path.join(directory, d)))
    return os.path.join(directory, most_recent)


def mean_of_chunks(series, chunk_size):
    # Explicitly setting numeric_only to True to avoid FutureWarning about deprecation
    return series.groupby(np.arange(len(series)) // chunk_size).mean(numeric_only=True)



# %%
"""
This chunk of retrieves and parses data from the folders
"""

path = f"{BASE_FOLDER}/output/host/workload=bitbrains-small/seed=0/"
print(f"Current working directory: {os.getcwd()}")
print(path)
recent_dir = most_recent_directory(path)
simulation_files = os.listdir(recent_dir)

simulation_data = []

for file in simulation_files:
    file_path = os.path.join(recent_dir, file)
    print(f"Reading file: {file_path}")
    try:
        data = pd.read_parquet(file_path)
        print(f"Successfully read file: {file_path}")
    except Exception as e:
        print(f"Failed to read file: {file_path}")
        print(f"Error: {e}")
        continue

    try:
        chunked_mean = mean_of_chunks(data, CHUNK_SIZE)
        print(f"Successfully computed mean of chunks for file: {file_path}")
    except Exception as e:
        print(f"Failed to compute mean of chunks for file: {file_path}")
        print(f"Error: {e}")
        continue

    simulation_data.append(chunked_mean)

# %%
