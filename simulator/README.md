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
	<a href="https://opensource.org/licenses/MIT">
	    <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="MIT License">
	</a>
</p>

## Introduction
This directory hosts the source code and development of the simulation component of the [OpenDC](https://opendc.org) project. 
This component is responsible for modelling and simulation of datacenters and their components. We have structured the directory into several subprojects:
- **[opendc-core](opendc-core)**  
  This module establishes core concepts and terminology of datacenters that we share across the various modules. 
  Other `opendc` modules build on these concepts and extend it in various directions (e.g. virtual machines or workflows).
- **[opendc-compute](opendc-compute)**  
  The [Infrastructure as a Service](https://en.wikipedia.org/wiki/Infrastructure_as_a_Service) (IaaS) component of OpenDC for computing infrastructure (similar to 
  [Amazon EC2](https://aws.amazon.com/ec2/) and [Google Compute Engine](https://cloud.google.com/compute)).
- **[opendc-workflows](opendc-workflows)**  
  Workflow orchestration service built on top of OpenDC. 
- **[opendc-format](opendc-format)**  
  Collection of libraries for processing data formats related to (simulation of) cloud computing and datacenters. 
- **[opendc-experiments](opendc-experiments)**  
  Collection of experiments that use OpenDC to explore various concepts of cloud computing and datacenters.
- **[opendc-runner-web](opendc-runner-web)**  
  Experiment runner that integrates with the OpenDC web interface.
- **[opendc-simulator](opendc-simulator)**  
  Collection of libraries that enable simulation of various components of datacenters. 
- **[opendc-utils](opendc-utils)**  
  Collection of utilities that are shared across the OpenDC subprojects.
  
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
