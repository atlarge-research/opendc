---
description: How to report a problem with OpenDC, propose a change, and contribute code or documentation.
---

# Contributing

OpenDC is an open-source project, and contributions are welcome. A contribution can be a bug report, a proposal for
new functionality, an improvement to the code or to this documentation, or an answer to another user's question. This
page explains how to make each kind of contribution, and how a release is made.

## Reporting a bug or proposing a feature

Bugs and feature requests are tracked as [issues on GitHub](https://github.com/atlarge-research/opendc/issues). Before
opening an issue, search the existing ones. If an issue already describes the problem, add a comment with further
information, or a thumbs-up reaction to show that it matters. Both help the maintainers to set priorities.

A good bug report describes the following:

- what was done, ideally with the smallest experiment file that shows the problem
- what was expected to happen
- what happened instead, with the complete error message
- the version of OpenDC and of Java

Questions about the use of OpenDC fit better in the
[discussions](https://github.com/atlarge-research/opendc/discussions) of the repository.

## Contributing code or documentation

Changes reach OpenDC through pull requests:

1. Fork the [repository](https://github.com/atlarge-research/opendc) and create a branch for the change.
2. Make the change. [Development](index.md) explains how to set up the project.
3. Run `./gradlew build`, which checks the code style and runs the tests. For a change to the documentation, run
   `uvx zensical build` instead.
4. Open a pull request against the branch `master`. Describe what the change does and why it is needed, so that a
   reviewer understands the intent before reading the code.

For a larger change, open an issue first and describe the plan. This avoids work on a change that the maintainers
would want to be done differently.

### Commit messages

Commit messages follow the [common conventions of Git](https://cbea.ms/git-commit/): a short summary line in the
imperative, a blank line and a body that explains what changed and why. Recent history also follows
[Conventional Commits](https://www.conventionalcommits.org), which prefixes the summary with the kind of change and the
module:

``` text
feat(opendc-sdk-python): Python SDK with disk traces, server runs and PyPI packaging
```

### Licence

OpenDC is distributed under the [MIT licence](https://github.com/atlarge-research/opendc/blob/master/LICENSE.txt).
Every source file starts with the licence header, which `./gradlew spotlessApply` adds.

## Releasing

A release is made by the maintainers in three steps, of which the workflows of the repository perform most:

1. A maintainer pushes a tag that starts with `v`, such as `v3.0`. The workflow `release.yml` builds the distributions
   with the version of the tag and drafts a release on GitHub with the distributions attached. A tag that contains
   `-rc` or `-m` gives a pre-release.
2. The maintainer reviews the draft, writes the release notes and publishes the release.
3. The publication starts the workflow `publish.yml`. It publishes the container images of the web application, the
   libraries of the SDK to Maven Central, and the Python SDK to the Python Package Index.

Between releases, the code carries the snapshot version that `gradle.properties` states. The Python Package Index
receives tagged releases only.
