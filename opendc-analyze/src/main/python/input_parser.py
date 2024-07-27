import json
import os
import sys
import warnings


def read_input(path=""):
    if not path:
        raise ValueError("No input path provided.")

    path = path.strip().strip(',')

    project_root = find_root_dir()
    if not project_root:
        raise ValueError("Project root not found.")

    full_path = os.path.join(project_root, path)

    if not os.path.exists(full_path):
        raise ValueError(f"File does not exist: {full_path}")

    try:
        with open(full_path, 'r') as raw_json:
            input_json = json.load(raw_json)
    except json.JSONDecodeError:
        raise ValueError("Failed to decode JSON.")
    except IOError:
        raise ValueError("MultiModel's parser says: Error opening file.")

    switch_to_root_dir()

    # Validate and apply defaults
    input_json = parse_input(input_json)
    return input_json


def parse_input(input_json):
    DEFAULTS = {
        "multimodel": True,
        "metamodel": False,
        "window_size": 1,
        "window_function": "mean",
        "meta_function": "mean",
        "samples_per_minute": 0,
        "current_unit": "",
        "unit_scaling_magnitude": 1,
        "plot_type": "time_series",
        "plot_title": "",
        "x_label": "",
        "y_label": "",
        "y_ticks_count": None,
        "x_ticks_count": None,
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
        raise ValueError("Required field 'metric' is missing.")

    if ("meta_function" not in input_json) and input_json["metamodel"]:
        raise ValueError("Required field 'meta_function' is missing. Please select between 'mean' and 'median'. Alternatively,"
              "disable metamodel in the config file.")

    if input_json["meta_function"] not in ["mean", "median", "meta_equation1", "equation2", "equation3"]:
        raise ValueError("Invalid value for meta_function. Please select between 'mean', 'median', !!!!!!!to be updated in the end!!!!!!!!.")

    # raise a warning
    if not input_json["multimodel"] and input_json["metamodel"]:
        warnings.warn("Warning: Cannot have a Meta-Model without a Multi-Model. No computation made.")

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
