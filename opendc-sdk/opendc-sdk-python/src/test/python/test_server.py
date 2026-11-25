"""`Server.simulate()` runs an experiment on an OpenDC server as `opendc run --api-url` does, here on a fake one."""

import http.client
import io
import json
import re
import zipfile
from pathlib import Path

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq
import pytest

import opendc
from fake_opendc_server import HANDLE, FakeOpendcServer
from fixtures import SINGLE_TASK, minute_export, single_task, tiny_experiment, tiny_scenario, tiny_topology
from opendc.server import RemoteState, unpack

pytestmark = pytest.mark.usefixtures("scratch")


@pytest.fixture(autouse=True)
def no_token(monkeypatch: pytest.MonkeyPatch) -> None:
    """Unsets OPENDC_TOKEN, which would sign in the requests of a test."""
    monkeypatch.delenv("OPENDC_TOKEN", raising=False)


def server(fake: FakeOpendcServer, *, token: str = "", patience: float = 120.0) -> opendc.Server:
    return opendc.Server(fake.url, token=token, poll_interval=0.001, patience=patience)


def trace_experiment(*names: str) -> opendc.KotlinObject:
    """An experiment with a workload of the trace referenced by each of `names`."""
    workloads = [opendc.trace_workload(opendc.NamedReference(name)) for name in names]
    return opendc.ExperimentSpec(topologies=[tiny_topology], workloads=workloads, export_models=[minute_export])


def posted(fake: FakeOpendcServer, experiment_id: str = "x-1") -> opendc.KotlinObject:
    """The spec of the experiment created as `experiment_id`."""
    return opendc.SdkJson.decode_experiment(json.dumps(fake.experiments[experiment_id]["spec"]))


def referenced(spec: opendc.KotlinObject) -> list[str]:
    return [use.reference.name for use in spec.references()]


def uploaded(fake: FakeOpendcServer, slug: str, table: str) -> pa.Table:
    return pq.read_table(io.BytesIO(fake.library()[slug].contents(table)))


def test_runs_the_experiment_and_returns_the_tables_of_its_runs() -> None:
    spec = tiny_experiment(name="tiny")

    with FakeOpendcServer() as fake:
        results = server(fake).simulate(spec)

    assert list(results.host.columns[:2]) == ["scenario", "seed"]
    assert results.host["cpu_usage"].tolist() == [1000.0, 500.0]
    assert list(results.scenarios.index) == [(0, 0)]
    assert fake.experiments["x-1"]["name"] == "tiny"
    assert posted(fake) == spec
    assert fake.cancelled == []


def test_gathers_runs_in_one_project_created_only_the_first_time() -> None:
    with FakeOpendcServer(statuses=["succeeded"]) as fake:
        server(fake).simulate(tiny_experiment())
        server(fake).simulate(tiny_experiment())

    assert fake.projects == ["opendc-python"]
    assert [experiment["projectId"] for experiment in fake.experiments.values()] == ["p-opendc-python"] * 2
    assert [experiment["name"] for experiment in fake.experiments.values()] == ["(unnamed)"] * 2


def test_runs_in_the_callers_own_project_rather_than_one_of_the_same_name_shared_with_them() -> None:
    with FakeOpendcServer(statuses=["succeeded"], shared_projects=["opendc-python"]) as fake:
        server(fake).simulate(tiny_experiment())

    assert fake.projects == ["opendc-python"]
    assert fake.experiments["x-1"]["projectId"] == "p-opendc-python"


@pytest.mark.parametrize(
    ("token", "environment", "authorization"),
    [
        ("", "", None),
        ("", "odc_pat_environment", "Bearer odc_pat_environment"),
        ("odc_pat_given", "odc_pat_environment", "Bearer odc_pat_given"),
    ],
)
def test_signs_in_with_the_token_given_or_else_the_one_in_the_environment(
    token: str, environment: str, authorization: str | None, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setenv("OPENDC_TOKEN", environment)

    with FakeOpendcServer(statuses=["succeeded"]) as fake:
        remote = server(fake, token=token)
        remote.simulate(tiny_experiment())

    assert {request.headers.get("Authorization") for request in fake.requests} == {authorization}
    assert "odc_pat" not in repr(remote)


def test_a_refused_experiment_raises_with_its_issues_and_leaves_no_draft_behind() -> None:
    issue = ("workloads[0].source", "no trace called nightly in your library")

    with FakeOpendcServer(submit_issues=[issue]) as fake, pytest.raises(opendc.ApiFailure) as failure:
        server(fake, token="odc_pat_secret").simulate(tiny_experiment())

    assert failure.value.status == 400
    assert failure.value.issues == ["workloads[0].source: no trace called nightly in your library"]
    assert "odc_pat_secret" not in str(failure.value)
    assert fake.deleted == ["x-1"]
    assert fake.status_reads == 0


def test_an_experiment_whose_submission_went_unanswered_is_deleted_rather_than_left_to_run_unwatched() -> None:
    with FakeOpendcServer(drops_submit=True) as fake, pytest.raises(http.client.RemoteDisconnected):
        server(fake).simulate(tiny_experiment())

    assert fake.deleted == ["x-1"]
    assert fake.status_reads == 0


def test_a_failed_run_raises_why_each_scenario_failed_and_fetches_no_archive() -> None:
    with FakeOpendcServer(statuses=["running", "failed"]) as fake, pytest.raises(opendc.RemoteRunFailed) as failure:
        server(fake).simulate(tiny_experiment())

    assert failure.value.state is RemoteState.FAILED
    assert failure.value.failures == ["scenario 0: simulationError (threw)"]
    assert len(failure.value.results) == 0
    assert list(failure.value.results.scenarios.index) == [(0, 0)]
    assert not any(path.endswith("/archive") for path in fake.paths("GET"))
    assert fake.cancelled == []


def test_a_partial_run_raises_with_the_tables_of_the_runs_that_landed() -> None:
    with FakeOpendcServer(statuses=["partial"]) as fake, pytest.raises(opendc.RemoteRunFailed) as failure:
        server(fake).simulate(tiny_experiment())

    assert failure.value.state is RemoteState.PARTIAL
    assert failure.value.failures == ["scenario 0: simulationError (threw)"]
    assert failure.value.results.host["cpu_usage"].tolist() == [1000.0, 500.0]


def test_rides_out_a_server_or_network_that_stops_answering_for_a_moment() -> None:
    with FakeOpendcServer(statuses=["running", "unavailable", "dropped", "unavailable", "succeeded"]) as fake:
        results = server(fake).simulate(tiny_experiment())

    assert len(results.host) == 2
    assert fake.cancelled == []


def test_cancels_the_run_when_the_server_stays_unavailable_for_longer_than_the_patience() -> None:
    with FakeOpendcServer(statuses=["running", "unavailable"]) as fake, pytest.raises(opendc.ApiFailure) as failure:
        server(fake, patience=0.05).simulate(tiny_experiment())

    assert failure.value.status == 503
    assert fake.status_reads > 2
    assert fake.cancelled == ["x-1"]


def test_cancels_the_run_when_following_it_is_interrupted() -> None:
    # Ends failed after the interrupt, so that a test that is not interrupted fails rather than hangs
    with FakeOpendcServer(statuses=["running", "interrupt", "failed"]) as fake, pytest.raises(KeyboardInterrupt):
        server(fake).simulate(tiny_experiment())

    assert fake.cancelled == ["x-1"]


@pytest.mark.parametrize("entry", ["../escaped.parquet", "tiny/../../escaped.parquet"])
def test_refuses_an_archive_that_names_a_path_outside_where_it_is_unpacked(tmp_path: Path, entry: str) -> None:
    archive = tmp_path / "archive.zip"
    with zipfile.ZipFile(archive, "w") as files:
        files.writestr("tiny/raw-output/0/seed=0/host.parquet", b"PAR1")
        files.writestr(entry, b"PAR1")

    with pytest.raises(ValueError, match="outside"):
        unpack(archive, tmp_path / "root")

    assert not (tmp_path / "escaped.parquet").exists()
    assert not (tmp_path / "root").exists()


def test_uploads_the_traces_the_experiment_references_and_runs_it_on_them() -> None:
    spec = trace_experiment("nightly", "bitbrains-small")
    trace = single_task()

    with FakeOpendcServer() as fake:
        server(fake).simulate(spec, traces={"nightly": trace, "unused": trace})

    [slug] = fake.library()
    assert re.fullmatch(rf"{HANDLE}/nightly-[0-9a-f]{{12}}", slug)
    assert fake.registrations == [slug]
    assert referenced(posted(fake)) == [slug, "bitbrains-small"]
    # Cast as staging casts: the submission times of the DataFrame become milliseconds since the epoch
    assert uploaded(fake, slug, "tasks").schema.field("submission_time").type == pa.timestamp("ms", tz="UTC")
    assert uploaded(fake, slug, "fragments").column("cpu_usage").to_pylist() == [39600.0]


def test_uploads_a_trace_once_for_each_of_its_contents() -> None:
    with FakeOpendcServer(statuses=["succeeded"]) as fake:
        for trace in (single_task(), single_task(), single_task(cpu_count=2)):
            server(fake).simulate(trace_experiment("nightly"), traces={"nightly": trace})

    assert len(fake.registrations) == 2
    assert referenced(posted(fake, "x-1")) == referenced(posted(fake, "x-2")) != referenced(posted(fake, "x-3"))


def test_uploads_to_storage_in_parts_and_without_the_credentials() -> None:
    trace = single_task()

    with FakeOpendcServer(uploads="storage") as fake:
        server(fake, token="odc_pat_secret").simulate(trace_experiment("nightly"), traces={"nightly": trace})

    [slug] = fake.library()
    assert len([path for path in fake.paths("PUT") if path.startswith("/storage/")]) > 2
    assert uploaded(fake, slug, "fragments").column("duration").to_pylist() == [3000000]


def test_an_upload_cut_short_is_sent_again_by_the_next_run() -> None:
    with FakeOpendcServer(failing_puts=1) as fake:
        with pytest.raises(opendc.ApiFailure, match="HTTP 500"):
            server(fake).simulate(trace_experiment("nightly"), traces={"nightly": single_task()})
        server(fake).simulate(trace_experiment("nightly"), traces={"nightly": single_task()})

    assert len(fake.registrations) == 2
    assert list(fake.library()) == fake.registrations[:1]
    assert list(fake.experiments) == ["x-1"]


def test_uploads_a_table_split_into_files_as_one_file(tmp_path: Path) -> None:
    tasks = pq.read_table(SINGLE_TASK / "tasks.parquet")
    split = tmp_path / "tasks.parquet"
    split.mkdir()
    pq.write_table(tasks.append_column("deadline", pa.array([60_000], pa.int64())), split / "part-0.parquet")
    pq.write_table(tasks.set_column(0, "id", pa.array([1], pa.int32())), split / "part-1.parquet")
    trace = opendc.WorkloadTrace(split, SINGLE_TASK / "fragments.parquet")

    with FakeOpendcServer() as fake:
        server(fake).simulate(trace_experiment("split"), traces={"split": trace})

    [slug] = fake.library()
    tasks = uploaded(fake, slug, "tasks")
    assert tasks.select(["id", "deadline"]).to_pydict() == {"id": [0, 1], "deadline": [60_000, None]}


def test_refuses_a_trace_of_another_kind_than_its_reference_before_sending_anything() -> None:
    carbon = opendc.CarbonTrace(pd.DataFrame({"timestamp": [pd.Timestamp("2022-01-01")], "carbon_intensity": [100.0]}))
    expected = r"traces\['nightly'\] is a carbon trace, but workloads\[0\]\.source references a workload trace"

    with FakeOpendcServer() as fake, pytest.raises(ValueError, match=expected):
        server(fake).simulate(trace_experiment("nightly"), traces={"nightly": carbon})

    assert fake.requests == []


def test_refuses_a_scenario_as_a_server_runs_experiments() -> None:
    with FakeOpendcServer() as fake, pytest.raises(TypeError, match="ExperimentSpec"):
        server(fake).simulate(tiny_scenario)

    assert fake.requests == []
