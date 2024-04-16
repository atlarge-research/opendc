# %%

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt


# %%

test = "test_1"

df_host = pd.read_parquet(f"output/{test}/seed=0/host.parquet")
df_server = pd.read_parquet(f"output/{test}/seed=0/server.parquet")
df_service = pd.read_parquet(f"output/{test}/seed=0/service.parquet")

plt.plot(df_service.servers_active)
plt.title("servers active")
plt.show()

plt.plot(df_host.cpu_usage)
plt.title("cpu usage")
plt.show()

# %%

df_service.hosts_up.plot()

# %%

df_meta = pd.read_parquet("traces/bitbrains-small/trace/meta.parquet")
# %%

df_meta.head()

# %%

df_trace = pd.read_parquet("traces/bitbrains-small/trace/trace.parquet")
# %%

df_trace.head()
# %%
