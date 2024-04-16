# %%

import pandas as pd
from datetime import datetime 
import pyarrow as pa
import pyarrow.parquet as pq

start_date = datetime.strptime("2024-2-1", "%Y-%m-%d")
end_date = datetime.strptime("2024-2-2", "%Y-%m-%d")

job_count = 10

schema_meta = {
    "id": pa.string(),
    "start_time": pa.timestamp("ms"),
    "stop_time": pa.timestamp("ms"),
    "cpu_count": pa.int32(),
    "cpu_capacity": pa.float64(),
    "mem_capacity": pa.int64()
}

pa_schema_meta = pa.schema([pa.field(x, y, nullable=False) for x, y in schema_meta.items()])

schema_trace = {
    "id": pa.string(),
    "timestamp": pa.timestamp("ms"),
    "duration": pa.int64(),
    "cpu_count": pa.int32(),
    "cpu_usage": pa.float64()
}

pa_schema_trace = pa.schema([pa.field(x, y, nullable=False) for x, y in schema_trace.items()])

# %%
#######################################################################################################################################
# TEST 1: parallel execution
#######################################################################################################################################

start_date = datetime.strptime("2024-2-1", "%Y-%m-%d")
end_date = datetime.strptime("2024-2-2", "%Y-%m-%d")
output_folder = "traces/single_task"

if not os.path.exists(f"{output_folder}/trace"):
    os.makedirs(f"{output_folder}/trace")

# create meta
meta_columns = ["id", "start_time", "stop_time", "cpu_count", "cpu_capacity", "mem_capacity"]
meta = [
    ["0", start_date, end_date, 1, 1000, 100_000]
]
df_meta = pd.DataFrame(meta, columns=meta_columns)

pa_meta_out = pa.Table.from_pandas(
    df = df_meta,
    schema = pa_schema_meta,
    preserve_index=False
)

pq.write_table(pa_meta_out, f"{output_folder}/trace/meta.parquet")


# create fragment

trace_columns = ["id", "timestamp", "duration", "cpu_count", "cpu_usage"]
trace = [
    ["0", end_date, 24*60*60*1000, 1, 1000]
]

df_trace = pd.DataFrame(trace, columns=trace_columns)

pa_trace_out = pa.Table.from_pandas(
    df = df_trace,
    schema = pa_schema_trace,
    preserve_index=False
)

pq.write_table(pa_trace_out, f"{output_folder}/trace/trace.parquet")


# %%
#######################################################################################################################################
# TEST 2: sequential tasks
#######################################################################################################################################

start_date = datetime.strptime("2024-2-1", "%Y-%m-%d")
end_date = datetime.strptime("2024-2-2", "%Y-%m-%d")
output_folder = "traces/muliple_tasks"

if not os.path.exists(f"{output_folder}/trace"):
    os.makedirs(f"{output_folder}/trace")

# create meta
meta_columns = ["id", "start_time", "stop_time", "cpu_count", "cpu_capacity", "mem_capacity"]
meta = [
    ["0", datetime.strptime("2024-2-1", "%Y-%m-%d"), datetime.strptime("2024-2-2", "%Y-%m-%d"), 1, 1000, 100_000],
    ["1", datetime.strptime("2024-2-2", "%Y-%m-%d"), datetime.strptime("2024-2-3", "%Y-%m-%d"), 1, 1000, 100_000],
    ["2", datetime.strptime("2024-2-1", "%Y-%m-%d"), datetime.strptime("2024-2-3", "%Y-%m-%d"), 1, 1000, 100_000]
]
df_meta = pd.DataFrame(meta, columns=meta_columns)

pa_meta_out = pa.Table.from_pandas(
    df = df_meta,
    schema = pa_schema_meta,
    preserve_index=False
)

pq.write_table(pa_meta_out, f"{output_folder}/trace/meta.parquet")


# create fragment

trace_columns = ["id", "timestamp", "duration", "cpu_count", "cpu_usage"]
trace = [
    ["0", datetime.strptime("2024-2-1", "%Y-%m-%d"), 24*60*60*1000, 1, 1000],
    ["1", datetime.strptime("2024-2-2", "%Y-%m-%d"), 24*60*60*1000, 1, 1000],
    ["2", datetime.strptime("2024-2-3", "%Y-%m-%d"), 48*60*60*1000, 1, 1000],
]

df_trace = pd.DataFrame(trace, columns=trace_columns)

pa_trace_out = pa.Table.from_pandas(
    df = df_trace,
    schema = pa_schema_trace,
    preserve_index=False
)

pq.write_table(pa_trace_out, f"{output_folder}/trace/trace.parquet")


# %%
