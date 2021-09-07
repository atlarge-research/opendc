# Radice
Data-driven Risk Analysis in Cloud Datacenters using Simulation.

## Generating the Experimental Data
This section describes how you can replicate the experimental results yourself.

### Prerequisites
In order to run the experiments, make sure you have the following software installed on your machine:

- A JDK distribution (minimum version 11), such as Oracle JDK or Azul Zulu.

### Running the experiments
You may run a portfolio as follows:

```BASH
bin/radice run -O output --repeats 4096 --parallelism 80 portfolios/PORTFOLIO.yml
```

This run all scenarios in the specified portfolio file 4096 times using 80 thread, 
writing the experimental data to the `output` directory. 
To get an indication of the total runtime, each replication takes approximately 30 seconds on a modern machine.

### (Re-)generating the portfolios
If you have made changes to the source code or default configuration, make sure
you (re-)generate the portfolios as follows:

```bash
mkdir -p portfolios
bin/radice generate -O portfolios/01-baseline.yml --portfolio baseline
bin/radice generate -O portfolios/04-workload.yml --portfolio workload
bin/radice generate -O portfolios/05-scheduler.yml --portfolio scheduler
```

## License

OpenDC and Radice are distributed under the MIT license. See [LICENSE-OpenDC.txt](/LICENSE.txt) and [LICENSE.txt](LICENSE.txt).
