<a href="https://opendc.org/">
    <img src="https://opendc.org/img/logo.png" alt="OpenDC logo" title="OpenDC" align="right" height="100" />
</a>

# OpenDC

Collaborative Datacenter Simulation and Exploration for Everybody

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](/LICENSE.txt)
[![Documentation](https://img.shields.io/badge/docs-master-green.svg)](./docs)
[![GitHub release](https://img.shields.io/github/release/atlarge-research/opendc)](https://github.com/atlarge-research/opendc/releases)
[![Build](https://github.com/atlarge-research/opendc/actions/workflows/build-simulator.yml/badge.svg)](https://github.com/atlarge-research/opendc/actions/workflows/build-simulator.yml)

-----

OpenDC is an open-source simulation platform for datacenters aimed at both research and education.

![Datacenter construction in OpenDC](docs/images/opendc-frontend-construction.png)

Users can construct datacenters (see above) and define portfolios of scenarios (experiments) to see how these
datacenters perform under different workloads and schedulers (see below).

![Datacenter simulation in OpenDC](docs/images/opendc-frontend-simulation.png)

The simulator is accessible both as a ready-to-use website hosted by us at [opendc.org](https://opendc.org), and as
source code that users can run locally on their own machine, through Docker.

🛠 OpenDC is a project by the [@Large Research Group](https://atlarge-research.com).

🐟 OpenDC comes bundled
with [Capelin](https://repository.tudelft.nl/islandora/object/uuid:d6d50861-86a3-4dd3-a13f-42d84db7af66?collection=education)
, the capacity planning tool for cloud datacenters based on portfolios of what-if scenarios. More information on how to
use and extend Capelin coming soon!

## Documentation

The documentation is located in the [docs/](docs) directory and is divided as follows:

1. [Deployment Guide](docs/deploy.md)
1. [Architectural Overview](docs/architecture.md)
1. [Contributing Guide](CONTRIBUTING.md)

## Contributing

Questions, suggestions and contributions are welcome and appreciated!
Please refer to the [contributing guidelines](CONTRIBUTING.md) for more details.

## License

OpenDC is distributed under the MIT license. See [LICENSE.txt](/LICENSE.txt).
