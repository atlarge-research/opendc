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
([Actor model](https://en.wikipedia.org/wiki/Actor_model)).

## Getting Started

The instructions below are meant for when you would like to use `odcsim` separately from `opendc`. If you simply want to use the simulator, you do not need to follow them, please refer to the [main README](../README.md) for instructions.

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

