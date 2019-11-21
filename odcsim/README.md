<h1 align="center">
  <a href="http://opendc.org/">
    <img src="../misc/artwork/logo.png" width="100" alt="OpenDC">
  </a>
  <br>
  odcsim
</h1>

## Introduction
**odcsim** is a framework for discrete event simulation in Kotlin, used
by the [OpenDC](https://opendc.org) project.
Simulations are defined in terms of a hierarchical grouping of actors
and the interactions between these actors
([Actor model](https://en.wikipedia.org/wiki/Actor_model)), using
an API similar to [Akka](https://doc.akka.io/docs/akka/current/index.html).

## Documentation
Check out the [Getting Started](#getting-started) section for a quick
overview.
The documentation is located in the [docs/](docs/) directory and is divided as follows:
* [Main Concepts](docs/concepts.md)
* [Building a Model](docs/build.md)
* [Running a Model](docs/run.md)
* [API Reference](https://atlarge-research.github.io/opendc-simulator)
* [Contributing Guide](../CONTRIBUTING.md)

## Getting Started

### Installation
Please add the required packages as dependency in your project.
Releases are available in the [Maven Central Repository](https://search.maven.org/).

The package `odcsim-core` is required to construct a simulation model.
A `odcsim-engine-*` package is needed for running the simulation
model.

#### Gradle 
Groovy
```groovy
implementation 'com.atlarge.odcsim:odcsim-api:2.0.0'
runtime 'com.atlarge.odcsim:odcsim-engine-omega:2.0.0'
```
Kotlin
```groovy
implementation("com.atlarge.odcsim:odcsim-api:2.0.0")
runtime("com.atlarge.odcsim:odcsim-engine-omega:2.0.0")
```

#### Maven
```xml
<dependency>
   <groupId>com.atlarge.odcsim</groupId>
   <artifactId>odcsim-api</artifactId>
   <version>2.0.0</version>
</dependency>

<dependency>
   <groupId>com.atlarge.odcsim</groupId>
   <artifactId>odcsim-engine-omega</artifactId>
   <version>2.0.0</version>
</dependency>  
```

