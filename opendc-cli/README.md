# opendc-cli

Command-line interface for running OpenDC datacenter simulations. It builds into an `opendc`
executable that runs, validates and inspects experiment files.

## Installation

Build a runnable distribution and put it on your `PATH`:

```bash
./gradlew :opendc-cli:installDist
export PATH=$PATH:$(realpath opendc-cli/build/install/OpenDCExperimentRunner/bin)
```

You can now call `opendc` from anywhere. Re-run `installDist` after code changes to refresh the
binary.

## Experiment file

Every command takes an experiment JSON file. A minimal, self-contained one declares a topology, a
workload, and how to schedule and export it. Save this as `experiment.json`:

```json
{
    "name": "getting-started",
    "topologies": [
        {
            "clusters": [
                {
                    "name": "C01",
                    "hosts": [
                        {
                            "name": "H01",
                            "count": 4,
                            "cpu": { "coreCount": 16, "coreSpeed": "2.1 GHz" },
                            "memory": { "size": "64 GiB" },
                            "cpuPowerModel": {
                                "type": "linear",
                                "idlePower": "32 Watts",
                                "maxPower": "180 Watts"
                            }
                        }
                    ],
                    "powerSource": { "name": "grid", "maxPower": "10000 Watts" }
                }
            ]
        }
    ],
    "workloads": [
        {
            "type": "inline",
            "tasks": [
                {
                    "id": 0,
                    "name": "task-0",
                    "submissionTime": "0 minutes",
                    "duration": "10 minutes",
                    "cpuCoreCount": 2,
                    "cpuCapacity": "2.1 GHz",
                    "memory": "2 GiB",
                    "fragments": [
                        { "duration": "10 minutes", "cpuUsage": "2.1 GHz" }
                    ]
                }
            ]
        }
    ],
    "allocationPolicies": [
        { "type": "prefab", "scheduler": "Mem" }
    ],
    "exportModels": [
        { "exportInterval": "1 minutes", "printFrequency": null }
    ],
    "runs": 1
}
```

The inline workload keeps everything in one file. A larger study points `workloads` and
`powerSource` at trace and carbon files instead, and adds more topologies or allocation policies to
sweep; see `demo/sample.json` for such an experiment.

### Splitting a file with `importFrom`

Any object in the experiment (the experiment itself, a topology, a cluster, a host, a workload) can
take the rest of its fields from another file with an `importFrom` key, so shared pieces live in one
place and are reused:

```json
"topologies": [ { "importFrom": "topologies/surfsara.json" } ]
```

Keys written next to `importFrom` are kept and override the imported file, so you can adopt a file
and then adjust it. Overrides replace a key whole; there is no deep merge:

```json
{ "importFrom": "hosts/big-host.json", "count": 64 }
```

Every relative `importFrom` path is resolved against the directory of the file that writes it, never
against the directory you run `opendc` from. So `opendc run studies/big/experiment.json` reading
`"importFrom": "topologies/surfsara.json"` looks for `studies/big/topologies/surfsara.json` whether
you launch it from the repository root or from inside `studies/big`. Imports nest, and each file
resolves its own paths against its own directory, so a path inside `topologies/surfsara.json` is
relative to `studies/big/topologies/`, not to the experiment; absolute paths are taken as written.
The practical consequence is that an experiment tree is relocatable only as a unit: move an
experiment together with the files it imports and everything still resolves, but move or copy the
experiment file alone and its relative imports break with a "does not exist" error naming where it
looked. Keep imported files in a stable layout beneath the experiment and write each path relative to
the file that references it, not to your shell. (Under `--legacy` the rule differs: legacy
experiments resolve the topologies they reference against your current working directory, so run them
from the directory those paths were written for.)

## Commands

```
opendc run <experiment.json>        Simulate every scenario and write Parquet results.
opendc validate <experiment.json>   Parse the file and report configuration issues.
opendc show <experiment.json>       Print the datacenter topologies it declares.
```

Run any command with `--help` to see its options.

### run

`run` simulates each scenario in the experiment and writes per-run results, showing a live progress
dashboard. Common options:

```
-o, --output <dir>        Directory for the Parquet results (default: output).
-p, --parallelism <n>     Number of runs to simulate concurrently (default: 1).
    --writer-threads <n>  Threads writing each of the task and host files of a run (default: 1).
                          More speed up one large run on an otherwise idle machine; keep 1 when
                          running many runs in parallel.
    --no-progress         Disable the live dashboard (use for CI or piped output).
    --no-summary          Skip the in-memory metrics summary on very large sweeps.
```

```bash
opendc run experiment.json -o results -p 4
```

### Running on a server

`--api-url` runs the experiment on an OpenDC server instead of this machine, follows its progress in
the same dashboard, and downloads the results into `--output` in the layout a local run writes:

```bash
OPENDC_TOKEN=odc_pat_... opendc run experiment.json --api-url https://opendc.example.org -o results
```

```
    --api-url <url>       The server to run on.
    --project <id>        The project to run in (default: one called "opendc-cli", created once).
    --token <token>       A personal access token, also read from OPENDC_TOKEN. A server that signs
                          nobody in needs none.
```

The server checks the experiment as the web app would, so a workload has to name a trace the server
knows: one it ships, such as `bitbrains-small`, or one in your library by its full name
(`alice/nightly`). Local trace files are not uploaded. The exit status is 0 when every scenario
succeeded, 2 when the server refused the experiment (its reasons are printed), and 1 when a run
failed; interrupting the command cancels the experiment on the server.

### Option placement (a common trap)

`--legacy` and `--strict` belong to the root `opendc` command, not to the subcommands. They must
appear before the subcommand name. This fails:

```bash
opendc run experiment.json --legacy      # error: no such option: --legacy
```

Put the root flags first:

```bash
opendc --legacy run experiment.json
```

`--legacy` reads files written in the deprecated `opendc-experiments` JSON format; `--strict`
rejects experiment files that contain unknown keys instead of ignoring them.

## Debugging from IntelliJ

The project imports as a Gradle project, so no extra setup is needed.

1. Open `opendc-cli/src/main/kotlin/org/opendc/cli/Main.kt`.
2. Click the green run arrow in the gutter next to `fun main`, then choose
   `Modify Run Configuration...`.
3. Set `Program arguments` to the command you want to debug, for example
   `run opendc-cli/src/test/resources/experiments/tiny-experiment.json --no-progress`.
4. Set `Working directory` to the repository root.
5. Set breakpoints and start the configuration in Debug mode.

Pass `--no-progress` while debugging so the terminal dashboard does not compete with the IDE console
for the output stream.

## Advanced example

```bash
opendc --legacy run experiments/failure_experiment.json -o results -p 4 --no-progress
```

This reads `failure_experiment.json` as a deprecated `opendc-experiments` document (`--legacy`, placed
before `run` because it is a root flag), simulates its scenarios four at a time (`-p 4`), and writes
the Parquet results under `results/` (`-o results`). `--no-progress` turns off the interactive
dashboard so the plain log output is safe to capture in a file or CI pipeline.
