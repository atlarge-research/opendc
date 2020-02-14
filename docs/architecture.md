# 3. Architecture Overview
The OpenDC simulator has two main components: `odcsim`, a general framework for discrete event-based simulation, and `opendc` a collection of components using `odcsim` to simulate datacenters. While `opendc` is the focus of this project and thus these docs, we also shortly explain what `odcsim` looks like, to give you a better understanding of the other concepts.

`TODO: Add a schematic general overview here.`

## 3.1 `odcsim`
The `odcsim` framework has an API module (`odcsim-api`) and an module implementing this API (`odcsim-engine-omega`). 

### 3.1.1 `odcsim-api`
The API defines the behavior of any implementation adhering to the `odcsim` framework. It centers around a `SimulationEngine`, which can be created through a `SimulationEngineProvider`.

A simulation has a root process with certain `Behavior` (a coroutine). Processes have a `ProcessContext` which allow them to spawn other processes and open communication `Channel`s with other processes. Each of these `Channel`s has a `SendPort` and a `ReceivePort`.

### 3.1.2 `odcsim-engine-omega`
The implementation is an executable interpretation of this API. The main class is `OmegaSimulationEngine` and takes care of transmitting timestamped events between processes. It ensures that message delivery order is the same as sent order. 

You might note that the simulation framework and the simulator itself makes extensive use of Kotlin's coroutine feature. This is a paradigm for asynchronous programming. If you are not familiar with coroutines, we suggest having a look at the [Kotlin documentation on this](https://kotlinlang.org/docs/reference/coroutines-overview.html), especially their [quick start guide](https://kotlinlang.org/docs/tutorials/coroutines/coroutines-basic-jvm.html) and their [hands-on guide](https://play.kotlinlang.org/hands-on/Introduction%20to%20Coroutines%20and%20Channels/01_Introduction).

## 3.2 `opendc`
The `opendc` package consists of 

**TODO: What are the main components (odcsim-core, odcsim-engine, opendc-core, opendc-format, opendc-testkit, opendc-workflows, I assume)? Short description of each in a bullet list (or even a schema drawing, if you have the time). Why this division?**

**TODO: One section per component, explaining what their responsibility is and what other modules they talk to. In what kind of scenarios do you as OpenDC developer need to touch/change them?**

---
[< Previous](setup.md) | [Next >](run.md)
