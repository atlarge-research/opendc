---
description: The Gradle tasks that build, test and check OpenDC, and how to work on the web application from source.
---

# Building and Testing

All parts of OpenDC are built and tested through Gradle, including the frontend and the Python SDK. This page lists the
tasks that a developer needs most often, explains how tests and the code style are checked, and shows how to run the
web application from source.

## Common tasks

Tasks run on the whole repository, or on one module when its path precedes the task, as in
`./gradlew :opendc-sdk:opendc-sdk-runner:test`.

| Task                                  | Action                                                                              |
|---------------------------------------|-------------------------------------------------------------------------------------|
| `./gradlew build`                     | compiles the code, checks the code style and runs the tests                         |
| `./gradlew assemble`                  | compiles and packages the code, without checks or tests                             |
| `./gradlew test`                      | runs the tests                                                                      |
| `./gradlew spotlessApply`             | formats the code and adds missing licence headers                                   |
| `./gradlew :opendc-cli:installDist`   | installs the `opendc` command in `opendc-cli/build/install/`                        |
| `./gradlew assembleDist`              | builds the archives that a release distributes                                      |
| `./gradlew codeCoverageReport`        | runs the tests and writes one coverage report for all modules                       |
| `./gradlew dokkaGenerate`             | generates the API documentation of the libraries from their source comments         |

Gradle builds modules in parallel and caches the results of tasks, so a repeated build runs only the tasks whose
inputs changed.

## Tests

The tests of the Kotlin and Java modules use JUnit&nbsp;5. Gradle does not print the number of tests that ran. Each
module writes a report to `build/reports/tests/test/index.html` and the raw results to `build/test-results/test/`.

`--tests` selects tests by name:

``` bash
./gradlew :opendc-sdk:opendc-sdk-runner:test --tests "*Carbon*"
```

Two rules keep the tests runnable on every machine and on the Linux and Windows runners of the continuous
integration:

- No test may need Docker. The tests of the web server use an in-memory H2 database and an in-memory telemetry store,
  whereas a deployment uses PostgreSQL and Redis. Database migrations therefore have to stay within the SQL that both
  databases understand.
- No test may need files outside the repository. Traces that a test needs are part of its resources, and they have to
  be free to share.

## Code style

[Spotless](https://github.com/diffplug/spotless) enforces the code style, and `build` fails when a file violates it.
Spotless applies three rules:

- Kotlin code follows [ktlint](https://pinterest.github.io/ktlint/).
- Java code follows the [Palantir Java format](https://github.com/palantir/palantir-java-format).
- Every source file starts with the licence header in `buildSrc/src/main/kotlin/license.kt`.

`spotlessApply` fixes all three. Run it after adding or moving a file:

``` bash
./gradlew spotlessApply
```

The frontend and the Python SDK have their own tools, which the next sections name.

## Working on the web application

The web application consists of a server and a frontend, which run as two processes during development. The server
starts in the development mode of Quarkus:

``` bash
./gradlew :opendc-web:opendc-web-server:quarkusDev
```

In this mode, the server needs no further services. It uses an in-memory database, keeps files in a local folder, signs
everybody in as one anonymous user, and simulates on the local machine. It listens on port 8080 and reloads when its
source code changes. All data is lost when the server stops.

The frontend runs in the development server of Next.js, on port 3000:

``` bash
./gradlew :opendc-web:opendc-web-frontend:nextDev
```

Gradle downloads Node.js and pnpm for these tasks. With pnpm installed, the same tools run directly in the folder
`opendc-web/opendc-web-frontend`, which is quicker during frontend work:

| Command                  | Gradle task  | Action                                                        |
|--------------------------|--------------|---------------------------------------------------------------|
| `pnpm dev`               | `nextDev`    | starts the development server                                 |
| `pnpm test`              | `vitest`     | runs the tests with Vitest                                    |
| `pnpm lint`              | `biomeCheck` | checks the code and its format with Biome                     |
| `pnpm exec tsc --noEmit` |              | checks the types. `pnpm lint` does not check types.           |
| `pnpm format`            |              | formats the code with Biome                                   |
| `pnpm build`             | `nextBuild`  | builds the static site into `out/`, which the server can serve |

[REST API](../web/api.md#openapi-description) explains where the running server documents its interface, and
[Deployment](../web/deployment.md) how the parts are packaged and deployed.

## Working on the Python SDK

The Python SDK is the Gradle module `opendc-sdk:opendc-sdk-python`. Gradle runs its formatter, its type checker and its
tests through uv. [Internals](../python-sdk/internals.md#building-and-testing) lists the tasks.

## Continuous integration

GitHub Actions runs the following workflows, which are defined in `.github/workflows`:

| Workflow      | Runs on                               | Action                                                                          |
|---------------|---------------------------------------|---------------------------------------------------------------------------------|
| `build.yml`   | every pull request and push to `master` | builds and tests on Linux and Windows, starts the web server against PostgreSQL, tests the Python SDK against it, and builds the images |
| `release.yml` | a tag that starts with `v`            | builds the distributions and drafts a release on GitHub                         |
| `publish.yml` | a published release                   | publishes the images, the libraries to Maven Central and the Python SDK to the Python Package Index |
| `deploy.yml`  | a push to `prod`                      | updates the deployment at `app.opendc.org`                                      |

A pull request has to pass `build.yml`. Running `./gradlew build` before pushing finds most failures early.
