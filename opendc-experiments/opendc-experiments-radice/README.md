# RaDiCe
This document provides instructions on reproducing the results of the RaDiCe paper.

## Prerequisites
In order to run the experiments, make sure Docker or Podman is installed on your machine. If you want to run the
experiments without using Docker, please make sure a JDK distribution (minimum version 11), such as Oracle JDK or Azul
Zulu, is installed on your system.

Before running the experiments, make sure you have downloaded and extracted the workload traces into the `traces`
directory of the current working directory.

## Generating the Experimental Data
This section describes how you can reproduce the experimental results yourself.
If you are only interested in generating the plots or exploring the data, refer to [Plotting the Results](#Plotting-the-Results).

You may run the experiments as follows:

```bash
mkdir -p data
docker run -it --rm \
  -e REPEATS=4096 \
  -e PARALLELISM=80 \
  --mount type=bind,source="$(pwd)"/data,target=/opt/radice/data \
  --mount type=bind,source="$(pwd)"/traces,target=/opt/radice/traces \
  ghcr.io/atlarge-research/radice \
  scripts/run-portfolios.sh
```

This will run all scenarios of the considered portfolios 4096 times using 80 threads, writing the experimental data to
the `data` directory. To get an indication of the total runtime, each experiment run takes approximately 30 seconds on
a modern machine.

To run instead the performance comparison against CloudSim Plus:
```bash
mkdir -p data
docker run -it --rm \
  --mount type=bind,source="$(pwd)"/data,target=/opt/radice/data \
  --mount type=bind,source="$(pwd)"/traces,target=/opt/radice/traces \
  ghcr.io/atlarge-research/radice \
  scripts/run-perf.sh
```

Note that the use of Docker might distort the results. The results in the paper were obtain from running the experiments
directly on bare-metal. 


## Plotting the Results

Start the RaDiCe Docker image as follows:

```bash
mkdir -p figures
docker run -it -p 8888:8888 --rm \
  --name radice-plots \
  --mount type=bind,source="$(pwd)"/data,target=/opt/radice/data \
  --mount type=bind,source="$(pwd)"/figures,target=/opt/radice/figures \
  ghcr.io/atlarge-research/radice
```

This will launch a Jupyter Notebook instance on your device, which can be used to generate the plots. 
To find the link to the Jupyter Notebook instance, open the logs of the container:

```
docker logs radice-plots
```


## License

OpenDC and Radice are distributed under the MIT license. See [LICENSE-OpenDC.txt](/LICENSE.txt) and [LICENSE.txt](LICENSE.txt).
