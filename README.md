<h1 align="center">
  <a href="http://opendc.org/">
    <img src="misc/artwork/logo.png" width="100" alt="OpenDC">
  </a>
  <br>
  OpenDC Simulator
</h1>
<p align="center">
Collaborative Datacenter Simulation and Exploration for Everybody
</p>
<p align="center">
	<a href="https://travis-ci.org/atlarge-research/opendc-simulator">
		<img src="https://travis-ci.org/atlarge-research/opendc-simulator.svg?branch=master" alt="Build Status">
	</a>
	<a href="https://opensource.org/licenses/MIT">
	    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="MIT License">
	</a>
</p>

## Introduction
This repository hosts the source code and development of the simulation component of the [OpenDC](https://opendc.org) project. This component is responsible for modelling and simulation of datacenters and their components. We have structured the repository into two individual subprojects:
- **[odcsim](/odcsim)**  
  A framework for discrete event simulation using the [Kotlin](https://kotlinlang.org/) language.
- **[opendc](/opendc)**  
  A collection of models for simulating clouds, datacenters and their components using the **odcsim** framework.

## Documentation
Check out the [Getting Started](#getting-started) section for a quick
overview.
The documentation is located in the [docs/](docs/) directory and is divided as follows:
1. [Toolchain Setup](docs/setup.md)
2. [Architecture Overview](docs/architecture.md)
3. [Pre-built Models](docs/models.md)
4. [Running a Model](docs/run.md)
5. [Building Your Own Model](docs/build.md)
6. [API Reference](https://atlarge-research.github.io/opendc-simulator) **(TODO maybe remove this until we have created this - see #36)**

## Contributing
### Contributing Guide
Read our [contributing guide](CONTRIBUTING.md) to learn about our
development process, how  to propose bug fixes and improvements, and how
to build and test your changes to the project.

### License
The OpenDC simulator is available under the [MIT license](https://github.com/atlarge-research/opendc-simulator/blob/master/LICENSE.txt).
