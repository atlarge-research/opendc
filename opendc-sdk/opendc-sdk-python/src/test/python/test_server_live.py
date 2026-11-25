"""`Server.simulate()` against the OpenDC server at the origin in OPENDC_TEST_SERVER, such as one on localhost:8080."""

import os

import pytest

import opendc
from fixtures import single_task, tiny_experiment, trace_host
from opendc import minutes

ORIGIN = os.environ.get("OPENDC_TEST_SERVER", "")

pytestmark = pytest.mark.skipif(not ORIGIN, reason="OPENDC_TEST_SERVER names no server to run on")


def test_runs_an_inline_workload_as_a_local_simulation_does() -> None:
    spec = tiny_experiment()

    results = opendc.Server(ORIGIN, poll_interval=0.2).simulate(spec)

    assert results.service["tasks_completed"].iloc[-1] == 1
    assert len(results.host) == len(opendc.simulate(spec).host)


def test_runs_a_workload_trace_passed_as_dataframes() -> None:
    topology = opendc.TopologySpec([opendc.DataCenterSpec([opendc.ClusterSpec(name="C01", hosts=[trace_host])])])
    spec = opendc.ExperimentSpec(
        topologies=[topology],
        workloads=[opendc.trace_workload(opendc.NamedReference("single-task"))],
        export_models=[opendc.ExportSpec(export_interval=minutes(10), print_frequency=None)],
    )

    results = opendc.Server(ORIGIN, poll_interval=0.2).simulate(spec, traces={"single-task": single_task()})

    assert results.service["tasks_completed"].iloc[-1] == 1
    assert set(results.task["task_id"]) == {0}
