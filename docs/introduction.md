# 1. Introduction
This repository hosts the source code and development of the simulation component of the [OpenDC](https://opendc.org) project. OpenDC is a simulation platform for cloud infrastructure, with the aim of making datacenter concepts and technology accessible to everyone. To learn more about OpenDC, have a look through our [vision paper](https://ieeexplore.ieee.org/document/8121623)!

The simulator is one of the components of the OpenDC stack. It is responsible for modelling and simulation of datacenters and their components. This entails receiving environments and experiment configurations from up the stack, simulating these configurations, and reporting back results.

The simulator is composed of two main components:
- **[odcsim](/odcsim)**  
  A framework for discrete event simulation.
- **[opendc](/opendc)**  
  A collection of models for simulating clouds, datacenters and their components, using the **odcsim** framework.

In the remainder of this documentation, we walk you through a typical toolchain setup for amending OpenDC in your own way, explain the architecture of the system, and show you how to run your own experiments with the simulator.

---
[Next >](setup.md)
