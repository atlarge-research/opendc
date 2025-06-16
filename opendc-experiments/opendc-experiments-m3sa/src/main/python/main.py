from models import MultiModel, MetaModel
from util import SimulationConfig, parse_configuration
from argparse import ArgumentParser, Namespace


def arg_parser() -> Namespace:
    parser = ArgumentParser(prog="m3sa", description="Multi-Model Simulation and Analysis")
    parser.add_argument("config", help="Path to the JSON configuration file", type=str)
    parser.add_argument("simulation", help="Path to the simulation directory", type=str)
    parser.add_argument("-o", "--output", help="Path to the output directory", type=str, nargs="?")
    return parser.parse_args()


def main():
    arg_input: Namespace = arg_parser()
    output_path: str = arg_input.output if arg_input.output else "output"
    simulation_path: str = arg_input.simulation
    simulation_config: SimulationConfig = parse_configuration(arg_input.config, output_path, simulation_path)

    multi_model: MultiModel = MultiModel(config=simulation_config)
    multi_model.generate_plot()

    if simulation_config.is_metamodel:
        meta_model: MetaModel = MetaModel(multi_model)
        meta_model.compute()
        meta_model.output()


if __name__ == "__main__":
    main()
