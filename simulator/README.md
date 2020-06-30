<h1 align="center">
  <a href="http://opendc.org/">
    <img src="../misc/artwork/logo.png" width="100" alt="OpenDC">
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
The documentation is located in the [docs/](docs) directory and is divided as follows:
1. [Introduction](docs/introduction.md)
1. [Toolchain Setup](docs/setup.md)
2. [Architecture Overview](docs/architecture.md)
3. [Running an Experiment](docs/run.md)

## Getting Started
TODO
