---
description: Failure models, which decide when hosts fail, for how long, and how many.
---

# Failure Model

Hosts fail, and how a datacenter copes with failures is part of its design. A *failure model* decides when hosts fail,
how long a failure lasts, and how many hosts it affects. This page describes the five kinds of failure model and the
format of failure traces.

OpenDC models start-stop failures. When a host fails, its tasks are interrupted, and the host cannot be used until the
failure ends. An interrupted task starts again, from the beginning or from its last
[checkpoint](checkpoint-model.md). OpenDC terminates a task that has failed
[`maxNumFailures`](index.md#fields) times.

The field `type` selects the kind of failure model:

| Type           | Failures are                                                     |
|----------------|------------------------------------------------------------------|
| `none`         | absent. This is the default.                                     |
| `prefab`       | sampled from a built-in model that is based on measured failures |
| `custom`       | sampled from three distributions that the experiment states      |
| `trace`        | replayed from a failure trace                                    |
| `inline-trace` | replayed from a list in the experiment                           |

## Custom models

A custom model samples every failure from three distributions:

| Field          | Distribution of                                                             |
|----------------|-----------------------------------------------------------------------------|
| `interArrival` | the time from the end of one failure to the start of the next, in hours     |
| `duration`     | the duration of a failure, in hours                                         |
| `hostFraction` | the fraction of the hosts that fail, limited to the range from 0 to 1       |

Each field is one of the following distributions:

| Type          | Fields             |
|---------------|--------------------|
| `constant`    | `value`            |
| `exponential` | `mean`             |
| `gamma`       | `shape`, `scale`   |
| `log-normal`  | `scale`, `shape`   |
| `normal`      | `mean`, `std`      |
| `pareto`      | `scale`, `shape`   |
| `uniform`     | `lower`, `upper`   |
| `weibull`     | `alpha`, `beta`    |

Custom models are random, so their results depend on the seed of the run. To obtain results that do not depend on one
seed, simulate each scenario several times with [`runs`](index.md#fields).

``` json
{
    "type": "custom",
    "interArrival": { "type": "exponential", "mean": 24.0 },
    "duration": { "type": "log-normal", "scale": 1.0, "shape": 0.5 },
    "hostFraction": { "type": "uniform", "lower": 0.1, "upper": 0.5 }
}
```

## Prefab models

A prefab is a custom model with fixed distributions, selected by name:

``` json
{ "type": "prefab", "prefabName": "G5k06Exp" }
```

The prefabs are fitted to the traces of the
[Failure Trace Archive](https://www.sciencedirect.com/science/article/pii/S0743731513000634). The name of a prefab
combines a trace with a family of distributions:

- the traces are `G5k06`, `Lanl05`, `Ldns04`, `Microsoft99`, `Nd07cpu`, `Overnet03`, `Pl05`, `Skype06` and
  `Websites02`;
- the families are `Exp` (exponential), `Wbl` (Weibull), `LogN` (log-normal) and `Gam` (gamma).

For example, `Lanl05Wbl` uses Weibull distributions fitted to the trace of Los Alamos National Laboratory from 2005.
Every prefab samples the fraction of failing hosts uniformly between 0 and 1.

Tables 1 and 2 list the parameters of the prefabs, in hours. The name of a prefab selects one row and one column in
each table. The parameters of a column are in the order of the fields of the corresponding
[distribution](#custom-models). `Lanl05Wbl`, for example, samples the time between two failures from a Weibull
distribution with an `alpha` of 0.48 and a `beta` of 816.60.

| Trace         | `Exp` (mean) | `Wbl` (alpha, beta) | `LogN` (scale, shape) | `Gam` (shape, scale) |
|---------------|--------------|---------------------|-----------------------|----------------------|
| `G5k06`       | 32.41        | 0.48, 14.37         | 1.51, 2.42            | 0.34, 94.35          |
| `Lanl05`      | 1779.99      | 0.48, 816.60        | 5.56, 2.39            | 0.35, 5102.71        |
| `Ldns04`      | 141.06       | 0.51, 79.30         | 3.25, 2.33            | 0.39, 362.43         |
| `Microsoft99` | 67.01        | 0.55, 35.30         | 2.62, 1.84            | 0.41, 162.19         |
| `Nd07cpu`     | 13.73        | 0.45, 4.16          | 0.30, 2.20            | 0.30, 46.16          |
| `Overnet03`   | 2.29         | 0.85, 2.04          | 0.19, 0.98            | 0.91, 2.53           |
| `Pl05`        | 159.49       | 0.33, 19.35         | 1.44, 2.86            | 0.20, 788.03         |
| `Skype06`     | 16.27        | 0.64, 10.86         | 1.60, 1.57            | 0.53, 30.79          |
| `Websites02`  | 11.85        | 0.46, 3.68          | 0.23, 2.02            | 0.31, 38.67          |

/// table-caption
The distributions of the time between two failures, in hours.
///

| Trace         | `Exp` (mean) | `Wbl` (alpha, beta) | `LogN` (scale, shape) | `Gam` (shape, scale) |
|---------------|--------------|---------------------|-----------------------|----------------------|
| `G5k06`       | 7.41         | 0.35, 0.47          | −2.00, 2.20           | 0.19, 39.92          |
| `Lanl05`      | 5.92         | 0.58, 2.18          | 0.05, 1.42            | 0.38, 15.44          |
| `Ldns04`      | 8.61         | 0.63, 5.62          | 0.91, 1.64            | 0.51, 16.87          |
| `Microsoft99` | 16.49        | 0.60, 9.34          | 1.42, 1.54            | 0.46, 35.52          |
| `Nd07cpu`     | 4.25         | 0.51, 0.74          | −1.02, 1.27           | 0.28, 15.07          |
| `Overnet03`   | 12.00        | 0.44, 2.98          | 0.08, 1.80            | 0.29, 41.64          |
| `Pl05`        | 49.61        | 0.36, 5.59          | 0.40, 2.45            | 0.21, 237.65         |
| `Skype06`     | 14.31        | 0.63, 9.48          | 1.40, 1.73            | 0.50, 28.53          |
| `Websites02`  | 1.18         | 0.65, 0.61          | −1.12, 1.13           | 0.50, 2.37           |

/// table-caption
The distributions of the duration of a failure, in hours.
///

## Trace models

A trace model replays the failures of a [failure trace](#failure-traces).

| Field        | Type                                                 | Default  | Description                                                                              |
|--------------|------------------------------------------------------|----------|------------------------------------------------------------------------------------------|
| `source`     | [trace reference](index.md#referencing-trace-files)  | required | The failure trace.                                                                       |
| `startPoint` | number                                               | `0.0`    | Where in the trace to start, as a fraction from 0 up to, but not including, 1. The failures before this point are replayed at the end. |
| `repeat`     | boolean                                              | `true`   | Whether the trace starts again from the beginning when it ends.                          |

``` json
{
    "type": "trace",
    "source": { "type": "named", "name": "failure_traces/my_failures.parquet" }
}
```

### Failure traces

A failure trace is a Parquet file with one row per failure. All three columns are required.

| Column              | Type   | Description                                                                                  |
|---------------------|--------|----------------------------------------------------------------------------------------------|
| `failure_interval`  | int64  | The time between the end of the previous failure and the start of this one, in milliseconds. |
| `failure_duration`  | int64  | How long the failure lasts, in milliseconds.                                                 |
| `failure_intensity` | double | The fraction of the hosts that fail.                                                         |

## Inline trace models

An inline trace model replays failures that the experiment lists itself, which suits tests and small studies. It has
the fields `startPoint` and `repeat` of a trace model, and a list `failures` in place of `source`. Each failure has
three fields:

| Field       | Type                    | Description                                                                 |
|-------------|-------------------------|-----------------------------------------------------------------------------|
| `interval`  | [duration](../units.md) | The time between the end of the previous failure and the start of this one. |
| `duration`  | [duration](../units.md) | How long the failure lasts. It has to be greater than 0.                    |
| `intensity` | number                  | The fraction of the hosts that fail, greater than 0 and at most 1.          |

``` json
{
    "type": "inline-trace",
    "failures": [
        { "interval": "6 hours", "duration": "30 minutes", "intensity": 0.25 },
        { "interval": "2 days", "duration": "1 hours", "intensity": 1.0 }
    ],
    "repeat": false
}
```
