---
description: How to install OpenDC locally, and start experimenting in no time.
---

# Installation

This page describes how to set up and configure a local single-user OpenDC installation so that you can quickly get your
experiments running. You can also use the [hosted version of OpenDC](https://app.opendc.org) to get started even
quicker.


## Prerequisites

1. **Supported Platforms**  
   OpenDC is actively tested on Windows, macOS and GNU/Linux.
2. **Required Software**  
   A Java installation of version 17 or higher is required for OpenDC. You may download the
   [Java distribution from Oracle](https://www.oracle.com/java/technologies/downloads/) or use the distribution provided
   by your package manager.

## Download

To get an OpenDC distribution, download a recent stable version from our [Releases](https://github.com/atlarge-research/opendc/releases)
page on GitHub.

## Setup

Unpack the downloaded OpenDC distribution and try the following command:

```bash
$ bin/opendc-server
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2022-09-12 10:30:22,064 INFO  [org.fly.cor.int.dat.bas.BaseDatabaseType] (main) Database: jdbc:h2:file:./data/opendc.db (H2 2.1)
2022-09-12 10:30:22,089 WARN  [org.fly.cor.int.dat.bas.Database] (main) Flyway upgrade recommended: H2 2.1.214 is newer than this version of Flyway and support has not been tested. The latest supported version of H2 is 2.1.210.
2022-09-12 10:30:22,098 INFO  [org.fly.cor.int.com.DbMigrate] (main) Current version of schema "PUBLIC": 1.0.0
2022-09-12 10:30:22,099 INFO  [org.fly.cor.int.com.DbMigrate] (main) Schema "PUBLIC" is up to date. No migration necessary.
2022-09-12 10:30:22,282 INFO  [org.ope.web.run.run.OpenDCRunnerRecorder] (main) Starting OpenDC Runner in background (polling every PT30S)
2022-09-12 10:30:22,347 INFO  [io.quarkus] (main) opendc-web-server 2.1-rc1 on JVM (powered by Quarkus 2.11.1.Final) started in 1.366s. Listening on: http://0.0.0.0:8080
2022-09-12 10:30:22,348 INFO  [io.quarkus] (main) Profile prod activated.
2022-09-12 10:30:22,348 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, flyway, hibernate-orm, hibernate-validator, jdbc-h2, jdbc-postgresql, kotlin, narayana-jta, opendc-runner, opendc-ui, resteasy, resteasy-jackson, security, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx]
```
This will launch the built-in single-user OpenDC server on port 8080. Visit
[http://localhost:8080](http://localhost:8080) to access the bundled web UI.

## Configuration

OpenDC can be configured using the configuration files located in the `conf` directory. By default, all user data is
stored in the `data` directory using the H2 database engine.

## Multi-tenant deployment

For more information on setting up multi-tenant, non-trivial deployments, see the [Deployment Guide](docs/advanced-guides/deploy.md).
