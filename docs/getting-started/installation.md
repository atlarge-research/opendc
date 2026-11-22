---
description: How to install OpenDC on a local machine.
---

# Installation

This page explains how to install the `opendc` command, which the rest of this section uses. The last section points
to the installation of the other entry points.

## Requirements

OpenDC runs on Linux, macOS and Windows, and needs Java&nbsp;21 or newer. Java is available from
[Adoptium](https://adoptium.net), from [Oracle](https://www.oracle.com/java/technologies/downloads/) and from the
package managers of most systems. `java -version` prints the installed version.

## Building the command

OpenDC&nbsp;3 is built from the `master` branch of the [OpenDC repository](https://github.com/atlarge-research/opendc):

``` bash
git clone https://github.com/atlarge-research/opendc.git
cd opendc
./gradlew :opendc-cli:installDist
```

The build writes OpenDC to `opendc-cli/build/install/OpenDCExperimentRunner/`. Its `bin` folder holds the `opendc`
command. To run `opendc` from any folder, add `bin` to the `PATH`:

=== "Linux and macOS"

    ``` bash
    export PATH=$PATH:$(realpath opendc-cli/build/install/OpenDCExperimentRunner/bin)
    ```

=== "Windows"

    ``` powershell
    $env:Path += ";$(Resolve-Path opendc-cli\build\install\OpenDCExperimentRunner\bin)"
    ```

    On Windows, the command is the script `opendc.bat` in the same folder.

To check that the installation works, print the help of the command:

``` bash
opendc --help
```

After pulling changes from the repository, run `installDist` again to update the command.

### Other ways to build

Besides `installDist`, Gradle can package OpenDC as an archive. All three tasks produce the same files: the `opendc`
command in `bin`, and the libraries it needs in `lib`.

| Task                                 | Result                                                             | Suited for                                                    |
|--------------------------------------|--------------------------------------------------------------------|---------------------------------------------------------------|
| `./gradlew :opendc-cli:installDist`  | a folder that is ready to run: `opendc-cli/build/install/OpenDCExperimentRunner/` | running OpenDC on the machine that built it. We recommend this task. |
| `./gradlew :opendc-cli:distTar`      | an archive: `opendc-cli/build/distributions/OpenDCExperimentRunner.tar` | copying OpenDC to another machine, such as a cluster, and unpacking it there with `tar -xf` |
| `./gradlew :opendc-cli:assembleDist` | two archives in `opendc-cli/build/distributions/`: `OpenDCExperimentRunner.tar` and `OpenDCExperimentRunner.zip` | sharing OpenDC with others, for example with Windows users, who unpack the `.zip` |

!!! note "OpenDC 2"

    To run OpenDC&nbsp;2 instead, see [Running OpenDC 2](../opendc-2/running.md).

## Other entry points

The other [entry points](entry-points.md) have their own installation:

- The Python SDK is a Python package. [Python SDK](../python-sdk/index.md#installation) explains how to install it.
- The Kotlin SDK is a dependency of a Gradle or Maven project. [Kotlin SDK](../kotlin-sdk/index.md#adding-the-sdk-to-a-project)
  explains how to add it.
- The web application runs in a browser. To host it, see [Deployment](../web/deployment.md).
