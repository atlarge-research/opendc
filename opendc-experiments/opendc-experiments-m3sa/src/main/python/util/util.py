from json import JSONDecodeError, load

UNIT_FACTORS: dict[int, str] = {
    -9: 'n',
    -6: 'μ',
    -3: 'm',
    0: '',
    1: 'k',
    3: 'M',
    6: 'G',
    9: 'T'
}

SIMULATION_ANALYSIS_FOLDER_NAME = 'simulation-analysis'
EMISSIONS_ANALYSIS_FOLDER_NAME = 'carbon_emission'
ENERGY_ANALYSIS_FOLDER_NAME = 'power_draw'

"""
SIMULATION_DATA_FILE (str): The name of the file containing the simulation data. Enter only the name, not the path, not
the extension. The data file must be parquet format.

✅ Good: "host", "simulation_data", "cats_predictions"
❌ Wrong: "host.json", "opendc/folder_x/folder_y/data"
"""
SIMULATION_DATA_FILE = "host"  # opendc outputs in file host.parquet


def adjust_unit(target_unit: str, magnitude: int) -> tuple[str, int]:
    """
    Adjusts the unit based on the magnitude provided.
    Example:
        adjust_unit('W', 3) -> ('kW', 1000)
    Args:
        target_unit: The target unit to adjust.
        magnitude: The magnitude to adjust the unit by.

    Returns:
        A tuple containing the adjusted unit and magnitude.
    """

    result_unit = UNIT_FACTORS.get(magnitude, '') + target_unit
    result_magnitude = (10 ** magnitude) if magnitude in UNIT_FACTORS else 1
    return result_unit, result_magnitude


def clean_analysis_file(metric: str) -> None:
    analysis_file_path = SIMULATION_ANALYSIS_FOLDER_NAME + "/"
    if metric == "power_draw":
        analysis_file_path += ENERGY_ANALYSIS_FOLDER_NAME
    else:
        analysis_file_path += EMISSIONS_ANALYSIS_FOLDER_NAME
    analysis_file_path += "/analysis.txt"

    with open(analysis_file_path, "w") as f:
        f.write("")


def parse_json(json_path: str) -> dict[str, any]:
    """
    Parses a JSON file and returns the dictionary representation.
    Args:
        json_path: The path to the JSON file.

    Returns:
        A dictionary containing the JSON data.
    """

    try:
        with open(json_path, 'r') as raw_json:
            return load(raw_json)
    except JSONDecodeError:
        print(f"Error decoding JSON in file: {json_path}")
        exit(1)
    except IOError:
        print(f"Error reading file: {json_path}")
        exit(1)
