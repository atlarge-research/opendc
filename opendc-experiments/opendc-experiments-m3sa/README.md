# M3SA — Multi-Meta-Model Simulation Analysis

M3SA runs OpenDC simulations and then performs Python-based analysis on the results.

## Quick Start with Docker

### Prerequisites
- Docker Desktop installed and **running**

### Build
From the **repository root**:
```bash
docker build -t opendc-m3sa -f opendc-experiments/opendc-experiments-m3sa/Dockerfile .
```

### Run
```bash
# Simulation only (no M3SA analysis)
docker run --rm \
  -v $(pwd)/data:/opt/opendc/data \
  -v $(pwd)/output:/opt/opendc/output \
  opendc-m3sa \
  --experiment-path data/scenario.json

# Simulation + M3SA analysis
docker run --rm \
  -v $(pwd)/data:/opt/opendc/data \
  -v $(pwd)/output:/opt/opendc/output \
  opendc-m3sa \
  --experiment-path data/scenario.json \
  --m3sa-setup-path data/m3sa_setup.json
```

### Using Docker Compose
```bash
docker compose -f docker-compose.m3sa.yml up --build
```

## Running Without Docker

### Prerequisites
- JDK 21
- Python 3.10+ with `pip`

### Setup Python dependencies
```bash
cd opendc-experiments/opendc-experiments-m3sa/src/main/python
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### Run via Gradle
```bash
./gradlew :opendc-experiments:opendc-experiments-m3sa:run \
  --args="--experiment-path data/scenario.json"
```

## M3SA Setup File

The M3SA setup JSON configures the analysis. See `src/test/resources/m3saSetups/` for examples.

Key fields:
| Field | Description |
|---|---|
| `metric` | Parquet column to analyze (e.g. `power_draw`, `energy_usage`) |
| `window_size` | Aggregation window size |
| `plot_type` | `time_series`, `cumulative`, or `cumulative_time_series` |
| `metamodel` | `true` to compute a meta-model across scenarios |
| `meta_function` | `mean` or `median` |
