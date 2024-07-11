import os
import json
import sys


def read_input(path=""):
    if not path:
        print("No input path provided.")
        sys.exit(1918)

    path = path.strip().strip(',')

    project_root = find_root_dir()
    if not project_root:
        print("Project root not found.")
        sys.exit(1)

    full_path = os.path.join(project_root, path)

    if not os.path.exists(full_path):
        print(f"File does not exist: {full_path}")
        sys.exit(1)

    try:
        with open(full_path, 'r') as raw_json:
            input_json = json.load(raw_json)
    except json.JSONDecodeError:
        print("Failed to decode JSON.")
        sys.exit(1)
    except IOError:
        print("MultiModel's parser says: Error opening file.")
        sys.exit(1)

    switch_to_root_dir()

    # Validate and apply defaults
    input_json = parse_input(input_json)
    return input_json


def parse_input(input_json):
    DEFAULTS = {
        "multimodel": True,
        "metamodel": False,
        "window_size": 1,
        "aggregation_function": "mean",
        "metamodel_function": "mean",
        "samples_per_minute": 0,
        "current_unit": "",
        "unit_scaling_magnitude": 0,
        "plot_type": "time_series",
        "plot_title": "",
        "x_label": "",
        "y_label": "",
        "y_min": None,
        "y_max": None,
        "x_min": None,
        "x_max": None,
    }

    # Apply default values where not specified
    for key, default_value in DEFAULTS.items():
        if key not in input_json:
            input_json[key] = default_value

    # Special handling for required fields without default values
    if "metric" not in input_json:
        print("Required field 'metric' is missing.")
        sys.exit(1)

    return input_json


def find_root_dir():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    root = os.path.abspath(os.sep)
    while current_dir and current_dir != root:
        if os.path.exists(os.path.join(current_dir, 'README.md')):
            return current_dir
        current_dir = os.path.dirname(current_dir)
    return None


def switch_to_root_dir():
    root_dir = find_root_dir()
    if root_dir:
        os.chdir(root_dir)
    else:
        print("Failed to switch to root directory.")
        sys.exit(1)
