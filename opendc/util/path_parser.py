import json
import os


def parse(version, endpoint_path):
    """Map an HTTP endpoint path to an API path"""

    # Get possible paths
    with open(os.path.join(os.path.dirname(__file__), '..', 'api', '{}', 'paths.json').format(version)) as paths_file:
        paths = json.load(paths_file)

    # Find API path that matches endpoint_path
    endpoint_path_parts = endpoint_path.strip('/').split('/')
    paths_parts = [x.strip('/').split('/') for x in paths if len(x.strip('/').split('/')) == len(endpoint_path_parts)]
    path = None

    for path_parts in paths_parts:
        found = True
        for (endpoint_part, part) in zip(endpoint_path_parts, path_parts):
            if not part.startswith('{') and endpoint_part != part:
                found = False
                break
        if found:
            path = path_parts

    if path is None:
        return None

    # Extract path parameters
    parameters = {}

    for (name, value) in zip(path, endpoint_path_parts):
        if name.startswith('{'):
            try:
                parameters[name.strip('{}')] = int(value)
            except:
                parameters[name.strip('{}')] = value

    return '{}/{}'.format(version, '/'.join(path)), parameters
