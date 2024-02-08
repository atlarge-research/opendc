# Setup Locally

This page describes steps to get started with OpenDC, by setting up a local development environment. In case any setup-related
issues are enocuntered, please view the [Troubleshooting](#troubleshooting) section.

# Requirements
OpenDC requires strict parameters to be run under normal conditions.
Failing to meet these conditions will most likely result in a failed compilation or compatibility issues.

1. `openjdk 17` or a similar `jdk` environment for **Java 17**
   <br>You can check your `jdk` version using:
```bash
java --version
```
2. A shell environment (terminal), able to run `bash` commands. We highly recommend Windows users to run OpenDC under [WSL](https://learn.microsoft.com/en-us/windows/wsl/install).
3. [OPTIONAL, YET RECOMMENDED] `maven 3.8` or a newer version. Check your `maven` version using:
```bash
mvn --version
```
<br>`maven` is used to handle java libraries, including our built version of OpenDC. `maven` usually comes preinstalled with most IDEs. Otherwise, you can install `maven` as follows:
```bash
# macOS
brew install maven
```
```bash
# Debian based linux distros
sudo apt install maven
```
```bash
# Arch Linux
sudo pacman -S maven
```

<hr>


# Setup

### 1. Clone the repository on your device

```bash
git clone https://github.com/atlarge-research/opendc.git
cd opendc/
```

### 2. Initialize the project using `gradle`
``` bash
./gradlew
```
Expected output: **BUILD SUCCESSFUL**.

### 3. Build OpenDC using `gradle`

```bash
./gradlew build
```
The execution should take $\approx 3min, depending on your machine. Expected output: **BUILD SUCCESSFUL**.

### 4. Disable key signing
i.e., remove GPG artifact signature application in `publishing-conventions.gradle.kts` in order to make publishing to Maven Local possible.

```bash
sed -i  "s/sign(publishing.publications)//g" buildSrc/src/main/kotlin/publishing-conventions.gradle.kts
```

**Note:** This command should not return anything. You can check if this step was processed by running:

```bash
grep -oq "sign(publishing.publications)" buildSrc/src/main/kotlin/publishing-conventions.gradle.kts && echo "Failed" || echo "Successful"
```

### 5. Run a greenifier experiment
#### 5.1 Navigate to opendc/experiments/greenifier/GreenifierCli.kt. 
#### 5.2 Run the main function
You should encounter an error message similar to the following:
```bash
simulating...   0% [                                   ]   0/128 (0:00:00 / ?) Exception in thread "main" java.io.FileNotFoundException
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
[...]
```
#### 5.3 Configure the 'run configuration'
5.3.1 In top right, click on "GreenifierCli" or on the down-pointing arrow next to it.

5.3.2 Click on the three dots near "GreenifierCli", then "Edit".

5.3.3 In the "Environment" tab, click on the plus sign, then "Environment variable".

5.3.4 In the "Program arguments" field add the following:
```bash
--env-path "resources/env" --trace-path "resources/bitbrains-small" -r 1 -p 1
```

5.3.5 In the "Working directory add" the following:
```bash
[YOUR ABSOLUTE PATH]/opendc-experiments/opendc-experiments-greenifier/src/main
```
Replace _[YOUR ABSOLUTE PATH]_ with your actual absolute path, such as:
```bash
/Users/radu/stuff/more-stuff/opendc-experiments/opendc-experiments-greenifier/src/main
```

5.3.6 Click "Apply", then "OK".

### 6. Create your own directory

Copy-paste the greenifier directory and rename it to your own name, following the same naming convention (e.g., "opendc-experiments-metamodel").
Now, go through all the files and replace every word of "greenifier" with your own name (e.g., "metamodel").

### 7. Run your own experiment
Navigate to your CLI file (e.g., "MetamodelCli.kt") and run the main function. Follow again step 5, and reconfigure the run configuration,
according to your own experiment.

### 8. Last / first step
This is the end of the very first step in your OpenDC journey. You can now start developing your own research project,
and contribute to a better, more powerful OpenDC platform.


<hr>

# Troubleshooting
All these steps were executed on commit [616017b](https://github.com/atlarge-research/opendc/tree/616017ba78a0882fe38b9b171b2b0f68e593cd8d).
We recommend running the steps on the latest commit from the master branch. If any issues are encountered, please report by creating a Github issue,
and run the steps on [616017b](https://github.com/atlarge-research/opendc/tree/616017ba78a0882fe38b9b171b2b0f68e593cd8d), or search for a more recent,
stable commit.

```bash 
git checkout 616017b
```

Authors: [Radu Nicolae](https://www.linkedin.com/in/rnicolae/), [Daniel Halasz](https://github.com/Stefan9110)
