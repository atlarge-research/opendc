# 3. Architecture Overview
The OpenDC simulator has two main components: `odcsim`, a general framework for discrete event-based simulation, and `opendc` a collection of components using `odcsim` to simulate datacenters. While `opendc` is the focus of this project and thus these docs, we also shortly explain what `odcsim` looks like, to give you a better understanding of the other concepts.

**TODO: Add a schematic general overview here.**

## 3.1 `odcsim`
The `odcsim` framework has an API module (`odcsim-api`) and an module implementing this API (`odcsim-engine-omega`). 

### 3.1.1 `odcsim-api`
The API defines the behavior of any implementation adhering to the `odcsim` framework. It centers around a `SimulationEngine`, which can be created through a `SimulationEngineProvider`.

A simulation consists of a number of simulation domains (logical processes) which itself consist of several coroutines
whose execution is serialized. That is, we guarantee that no two coroutines run at the same time.

### 3.1.2 `odcsim-engine-omega`
The implementation is an executable interpretation of this API. The main class is `OmegaSimulationEngine` and takes care of transmitting timestamped events between processes. It ensures that message delivery order is the same as sent order. 

You might note that the simulation framework and the simulator itself makes extensive use of Kotlin's coroutine feature. This is a paradigm for asynchronous programming. If you are not familiar with coroutines, we suggest having a look at the [Kotlin documentation on this](https://kotlinlang.org/docs/reference/coroutines-overview.html), especially their [quick start guide](https://kotlinlang.org/docs/tutorials/coroutines/coroutines-basic-jvm.html) and their [hands-on guide](https://play.kotlinlang.org/hands-on/Introduction%20to%20Coroutines%20and%20Channels/01_Introduction).

## 3.2 `opendc`
The `opendc` package consists of a number of submodules, the most feature-rich being `opendc-compute`. Below, we will explain each in turn.

### 3.2.1 `opendc-core`
This module defines a base model for datacenter simulation, establishing core concepts and terminology of datacenters
that we share across the various modules. Other `opendc` modules build on this model and extend it in various directions (e.g. virtual machines or workflows).

### 3.2.2 `opendc-compute`
This module is concerned with modeling cloud computing services (such as [Amazon EC2](https://aws.amazon.com/ec2/) and [Google Compute Engine](https://cloud.google.com/compute)) and provides the following features:

1. **Model for simulated workloads**  
   We represent workloads as bootable disk images (called `Image`) which characterize the runtime behavior
   of a running server in terms of the cpu time they require over time.
2. **Bare-metal management & provisioning**  
   We provide models for simulating management of bare-metal machines (`Node`) and deploying workloads on it (using `BareMetalDriver`). 
   Furthermore, we also include functionality for simulating and experimenting with (custom) provisioning policies on a pool of bare-metal machies (using `ProvisioningService`).
3. **Virtual machine management, scheduling and provisioning**  
   We provide functionality for simulating deployment of multiple `Image`s on a single machine using a hypervisor, which
   is concerned with scheduling/distributing the load of the running guest machines on the host machine. This may be used to experiment with overcommitting of virtual resources.
   Moreover, we also model provisioning policies for virtual machine provisioning with a pool of host machines. 

### 3.2.3 `opendc-workflows`
This module contains all workflow-related models and logic of the simulator. The models for workflows can be found in the `workload` package: A `Job` and a `Task`. The logic concerning the scheduling of a workflow is contained in the `service` package. It follows the Reference Architecture for Datacenter Schedulers by [Andreadis et al.](https://dl.acm.org/doi/10.5555/3291656.3291706). For a good introduction into datacenter schedulers and to fully grasp the modeling approach taken, we highly recommend reading this publication (or its more extensive [technical report](https://arxiv.org/pdf/1808.04224.pdf)).

The `service` package merits its own explanation. A scheduler is defined by the `StageWorkflowScheduler` class, according to the architecture. The main component, however, is the `StageWorkflowSchedulerLogic`, responsible for pulling together the different scheduling stage implementations from the `stage` package. The scheduler is managed by the `WorkflowService`, which also orchestrates the lifecycle of a workflow.

### 3.2.4 `opendc-format`
Running scientific experiments does not require running the full OpenDC stack. We also support directly reading out environment and workload trace files. Example implementations of these can be found in the `opendc-format` module. To parse a different format, you can take one of the existing parsers and adapt it to your needs. 

### 3.2.5 `opendc-experiments-sc18`
This is a module created for the experiments of our [SC18 publication](https://dl.acm.org/doi/10.5555/3291656.3291706). We aim to separate these kinds of custom experiment setups from the rest of the codebase by placing them in separate modules.

---
[< Previous](setup.md) | [Next >](run.md)
