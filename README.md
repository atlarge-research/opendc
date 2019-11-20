<h1 align="center">
  <a href="http://opendc.org/">
    <img src="misc/artwork/logo.png" width="100" alt="OpenDC">
  </a>
  <br>
  OpenDC
</h1>
<p align="center">
	<a href="https://travis-ci.org/atlarge-research/opendc-simulator">
		<img src="https://travis-ci.org/atlarge-research/opendc-simulator.svg?branch=master" alt="Build Status">
	</a>
	<a href="https://opensource.org/licenses/MIT">
	    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="MIT License">
	</a>
</p>
<p align="center">
    Collaborative Datacenter Simulation and Exploration for Everybody
</p>


## Introduction
**odcsim** is a framework for discrete event simulation in Kotlin and Java, used
by the [OpenDC](https://opendc.org) project.
Simulations are defined in terms of a hierarchical grouping of actors
and the interactions between these actors
([Actor model](https://en.wikipedia.org/wiki/Actor_model)), using
an API very similar to [Akka Typed](https://doc.akka.io/docs/akka/current/typed/index.html).

## Documentation
Check out the [Getting Started](#getting-started) section for a quick
overview.
The documentation is located in the [docs/](docs/) directory and is divided as follows:
* [Main Concepts](docs/concepts.md)
* [Building a Model](docs/build.md)
* [Running a Model](docs/run.md)
* [Pre-built Models](docs/models.md)
* [API Reference](https://atlarge-research.github.io/opendc-simulator)
* [Contributing Guide](CONTRIBUTING.md)

## Contributing
### Contributing Guide
Read our [contributing guide](CONTRIBUTING.md) to learn about our
development process, how  to propose bug fixes and improvements, and how
to build and test your changes to the project.

### License
The OpenDC simulator is [MIT licensed](https://github.com/atlarge-research/opendc-simulator/blob/master/LICENSE.txt).
