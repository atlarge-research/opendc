# OpenDC Documentation

OpenDC is a free and open-source platform for cloud datacenter simulation. It models
compute workloads, scheduling policies, power consumption, carbon emissions, and failures
using a discrete-event simulation engine.

This site is split into two kinds of pages:

- **Hand-written guides** (like this one) that explain concepts and workflows.
- **Generated reference** pages that are produced directly from the source code, so they
  never drift out of sync with the simulator.

## Reference

- [Experiment](reference/input/experiment.md) — the JSON schema for an experiment file.
- [Allocation policies](reference/input/allocation-scheduling.md) — allocation policies, filters and weighers.
- [Export models](reference/input/export.md) — how and what telemetry is exported.
- [Workloads](reference/input/workload.md) — workload traces and scaling policies.
- [Failure models](reference/input/failures.md) — failure and distribution specs.
- [Checkpointing models](reference/input/checkpointing.md) — checkpointing behaviour.
- [Output columns](reference/output-columns.md) — the columns of each Parquet output file.

## Regenerating the reference

The reference pages are generated from the code that defines them. To rebuild them:

```bash
./gradlew generateDocs
```

This rewrites the files under `docs/reference/`. Prose descriptions live in
`docs/descriptions/*.json` and are merged into the generated tables, so regenerating
never overwrites hand-written text.

### Splitting the input reference across files

By default the entire experiment schema is written to a single page. To split it,
create `docs/reference-layout.json` listing which types belong in which file. Files may live in
subfolders under `docs/reference/`:

```json
{
  "files": [
    { "file": "input/experiment.md", "title": "Experiment input format", "types": ["ExperimentSpec"] },
    { "file": "input/allocation-scheduling.md", "title": "Allocation Policies", "types": ["AllocationPolicySpec", "HostFilterSpec"] }
  ]
}
```

Listing a sealed type (e.g. `AllocationPolicySpec`) pulls its variants into the same file.
Any type you do not list goes to `defaultFile` (or, when that is unset, the first file). Links
between types in different files — including across subfolders — are rewritten automatically.
Remember to add any new files to the `nav` in `mkdocs.yml`.

## Previewing the site locally

```bash
pip install mkdocs-material
mkdocs serve
```
