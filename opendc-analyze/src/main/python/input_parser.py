import os
import json
import utils


def read_input(path=""):
    if (path == ""):
        os.exit(1918)

    # Remove any trailing commas or spaces
    path = path.strip().strip(',')

    # Ensure the path is relative to the project root
    project_root = find_root_dir()
    full_path = os.path.join(project_root, path)

    # Read the JSON file from the given path
    with open(full_path) as raw_json:
        parsed_json = json.load(raw_json)

    switch_to_root_dir()
    return parsed_json


def find_root_dir():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    while current_dir != os.path.dirname(current_dir):  # while not at the filesystem root
        if os.path.exists(os.path.join(current_dir, 'README.md')):  # or any other root-specific file or directory
            return current_dir
        current_dir = os.path.dirname(current_dir)


def switch_to_root_dir():
    root_dir = find_root_dir()
    os.chdir(root_dir)

