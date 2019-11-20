<h1 align="center">
  <a href="http://opendc.org/">
    <img src="../misc/artwork/logo.png" width="100" alt="OpenDC">
  </a>
  <br>
  odcsim
</h1>

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

## Getting Started

### Installation
Please add the required packages as dependency in your project.
Releases are available in the [Maven Central Repository](https://search.maven.org/).

The package `odcsim-core` is required to construct a simulation model.
A `odcsim-engine-*` package is needed for running the simulation
model.

**Gradle**
```groovy
compile 'com.atlarge.odcsim:odcsim-core:2.0.0'
compile 'com.atlarge.odcsim:odcsim-engine-omega:2.0.0'
```

**Maven**
```xml
<dependency>
   <groupId>com.atlarge.odcsim</groupId>
   <artifactId>odcsim-core</artifactId>
   <version>2.0.0</version>
</dependency>

<dependency>
   <groupId>com.atlarge.odcsim</groupId>
   <artifactId>odcsim-engine-omega</artifactId>
   <version>2.0.0</version>
</dependency>  
```

### Construction of Simulation Model
Let's construct a simple simulation model of a single car actor.
The car will alternately drive and park for a while. When it starts
driving (or parking), it will print the current simulation time.


```kotlin
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.coroutines.suspending
import com.atlarge.odcsim.coroutines.dsl.timeout

fun car(): Behavior<Nothing> = 
    suspending { ctx ->
        while (true) {
            println("Start parking at ${ctx.time}")
            val parkingDuration = 5.0
            timeout(parkingDuration)
             
            println("Start driving at ${ctx.time}")
            val tripDuration = 2.0
            timeout(tripDuration)
        }
        
        stopped()
    }
```

### Running Simulation
Running the constructed simulation model requires an implementation
of the `ActorSystem` interface provided by one of the `odcsim-engine-*`
packages. The [ServiceLoader](https://docs.oracle.com/javase/9/docs/api/java/util/ServiceLoader.html)
class found in the JDK can be used to locate the `ActorSystem` implementation on the classpath.
```kotlin
import com.atlarge.odcsim.ActorSystemFactory
import java.util.ServiceLoader

val factory = ServiceLoader.load(ActorSystemFactory::class.java).first()
val system = factory(car(), name = "car")
system.run(until = 10.0)
system.terminate()
```

