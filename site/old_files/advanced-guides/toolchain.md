---
sidebar_position: 1
---

# Toolchain Setup

The OpenDC simulator is built using the [Kotlin](https://kotlinlang.org/) language. This is a JVM-based language that
should appear familiar to programmers knowledgeable in Java or Scala. For a short interactive introduction to Kotlin,
the [Learn Kotlin By Example](https://play.kotlinlang.org/byExample/overview) docs are a great place to start.

For the build and dependency toolchain, we use [Gradle](https://gradle.org/). You will likely not need to change the
Gradle build configurations of components, but you will use Gradle to execute builds and tests on the codebase.

Follow the steps below to get it all set up!

## Contents

1. [Installing Java](#1-installing-java)
2. [Building and Developing](#2-building-and-developing)  
3. [Setup with IntelliJ IDEA](#21-setup-with-intellij-idea)
4. [Setup with Command Line](#22-setup-with-command-line)

## 1. Installing Java

OpenDC requires a Java installation of version 17 or higher. Make sure to install
the [JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html), not only the JRE (the JDK also includes
a JRE).

## 2. Building and Developing

With Java installed, we're ready to set up the development environment on your machine. You can either use a visual IDE
or work from a command line shell. We outline both approaches below, feel free to choose which you are most comfortable
with. If in doubt which one to choose, we suggest going with the first one.

## 2.1 Setup with IntelliJ IDEA

We suggest using [IntelliJ IDEA](https://www.jetbrains.com/idea/) as development environment. Once you have installed
any version of this IDE on your machine, choose "Get from Version Control" in the new project dialogue.
Enter `https://github.com/atlarge-research/opendc` as URL and submit your credentials when asked.
Open the project once it's ready fetching the codebase, and let it set up with the defaults (IntelliJ will recognize
that this is a Gradle codebase).

You will now be prompted in a dialogue to enable auto-import for Gradle, which we suggest you do. Wait for any progress
bars in the lower bar to disappear and then look for the Gradle simHyperVisorContext menu on the right-hand side. In it, go
to `opendc > Tasks > verification > test`. This will build the codebase and run checks to verify that tests
pass. If you get a `BUILD SUCCESSFUL` message, you're ready to go to the [next section](architecture)!

## 2.2 Setup with Command Line

First, clone the repository with the following command:

```shell script
git clone https://github.com/atlarge-research/opendc
```

And enter the directory:

```shell script
cd opendc
```

If on Windows, run the batch file included in the root, as follows:

```commandline
gradlew.bat test
```

If on Linux/macOS, run the shell script included in the root, as follows:

```shell script
./gradlew test
```

If the build is successful, you are ready to go to the next section!
