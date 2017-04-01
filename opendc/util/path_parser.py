import json
import sys
import re

def parse(version, endpoint_path):
    """Map an HTTP call to an API path"""

    with open('opendc/api/{}/paths.json'.format(version)) as paths_file:
        paths = json.load(paths_file)
    
    endpoint_path_parts = endpoint_path.split('/')
    paths_parts = [x.split('/') for x in paths if len(x.split('/')) == len(endpoint_path_parts)]

    for path_parts in paths_parts:
        found = True

        for (endpoint_part, part) in zip(endpoint_path_parts, path_parts):
            print endpoint_part, part
            if not part.startswith('{') and endpoint_part != part:
                found = False
                break
        
        if found:
            sys.stdout.flush()
            return '{}/{}'.format(version, '/'.join(path_parts))    

    return None
