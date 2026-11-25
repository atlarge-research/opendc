"""Runs experiments on an OpenDC server, as `opendc run --api-url` does, and returns their tables."""

from __future__ import annotations

import contextlib
import enum
import hashlib
import http.client
import json
import os
import re
import shutil
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from collections.abc import Iterable, Mapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any, BinaryIO

import jpype

from opendc.bridge import Builder, KotlinObject, sdk, to_java, to_python
from opendc.simulation import NO_TRACES, SimulationResults, expand, read_tables, scenario_frame
from opendc.traces import Trace, check_roles, check_traces

__all__ = ["ApiFailure", "RemoteRunFailed", "RemoteState", "RemoteStatus", "Server"]

JSON = "application/json"
ZIP = "application/zip"
OCTET_STREAM = "application/octet-stream"
USER_AGENT = "opendc-sdk-python"

TIMEOUT = 60.0
"""Seconds a request waits for the server to connect, or for the next bytes of its answer."""

SERVER_ERROR = 500

TRACE_KEYS = "org.opendc.sdk.python.TraceKeysKt"
"""The Kotlin functions that read the key of a trace reference and rename the references of a spec."""

TRACE_NAME_CAP = 100
"""The longest trace name the server accepts."""

DIGEST_LENGTH = 12


class ApiFailure(Exception):
    """An error answer from the server: its HTTP `status`, and the `title` and `issues` of its problem document."""

    def __init__(self, status: int, title: str, issues: list[str]) -> None:
        super().__init__("\n  ".join([f"{title} (HTTP {status})", *issues]))
        self.status = status
        self.title = title
        self.issues = issues


class RemoteState(enum.Enum):
    """The state of an experiment on a server."""

    DRAFT = "draft"
    QUEUED = "queued"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    PARTIAL = "partial"
    FAILED = "failed"
    CANCELLED = "cancelled"

    @property
    def terminal(self) -> bool:
        """Whether the experiment has ended."""
        return self in (RemoteState.SUCCEEDED, RemoteState.PARTIAL, RemoteState.FAILED, RemoteState.CANCELLED)


@dataclass(frozen=True)
class RemoteStatus:
    """The state and progress of an experiment on a server, and why each of its failed scenarios failed."""

    state: RemoteState
    completed_tasks: int
    total_tasks: int
    failures: list[str]


class RemoteRunFailed(Exception):
    """A run that did not fully succeed: why each scenario failed, and the `results` of the runs that succeeded."""

    def __init__(self, state: RemoteState, failures: list[str], results: SimulationResults) -> None:
        super().__init__("\n  ".join([f"The experiment ended {state.value}", *failures]))
        self.state = state
        self.failures = failures
        self.results = results


class Server:
    """An OpenDC server that runs experiments in the caller's project named `project`, created on first use."""

    def __init__(
        self,
        url: str,
        *,
        token: str = "",
        project: str = "opendc-python",
        poll_interval: float = 1.0,
        patience: float = 120.0,
    ) -> None:
        if urllib.parse.urlsplit(url).scheme not in ("http", "https"):
            raise ValueError(f"A server is reached at an http or https URL, not {url!r}")

        self.url = url.rstrip("/")
        self.project = project
        self.poll_interval = poll_interval
        self.patience = patience

        token = token or os.environ.get("OPENDC_TOKEN", "")
        self.credentials = {"Authorization": f"Bearer {token}"} if token else {}

    def __repr__(self) -> str:
        return f"Server({self.url!r}, project={self.project!r})"

    def simulate(self, spec: KotlinObject | Builder, *, traces: Mapping[str, Trace] = NO_TRACES) -> SimulationResults:
        """Runs an `ExperimentSpec`, or its builder, on the server and returns its tables, as `simulate()` does."""
        if isinstance(spec, Builder):
            spec = spec.build()
        if not isinstance(spec, sdk.ExperimentSpec):
            raise TypeError(f"A server runs an ExperimentSpec, or the builder of one, not {spec!r}")
        scenarios = expand(spec)
        check_traces(traces)
        uses = spec.references()
        check_roles(traces, uses)

        # A server runs only experiments whose traces are in the caller's library
        names = self.library_names(uses, traces)
        renamed = jpype.JClass(TRACE_KEYS).withLibraryNames(to_java(spec), to_java(names))
        experiment_id = self.start(to_python(renamed))

        try:
            status = self.follow(experiment_id)
        except BaseException:
            # A run nobody follows any more only uses up its owner's budget
            with contextlib.suppress(Exception):
                self.cancel(experiment_id)
            raise

        landed = status.state in (RemoteState.SUCCEEDED, RemoteState.PARTIAL)
        results = self.results(experiment_id, scenarios) if landed else SimulationResults({}, scenario_frame(scenarios))

        if status.state is not RemoteState.SUCCEEDED:
            raise RemoteRunFailed(status.state, status.failures, results)
        return results

    def library_names(self, uses: list[KotlinObject], traces: Mapping[str, Trace]) -> dict[str, str]:
        """Returns the library slug of each trace in `traces` that one of `uses` references, uploading new ones."""
        keys = [str(jpype.JClass(TRACE_KEYS).key(to_java(use.reference))) for use in uses]
        used = {key: traces[key] for key in keys if key in traces}
        return {key: self.upload_trace(key, trace) for key, trace in used.items()}

    def start(self, spec: KotlinObject) -> str:
        """Creates `spec` as a draft in the project and submits it, returning its id; a failed submit deletes it."""
        experiment_id = self.create_experiment(self.project_named(self.project), spec.name or "(unnamed)", spec)

        try:
            self.submit(experiment_id)
        except BaseException:
            # A draft the server refused, or queued as the answer was lost, would pile up or run unwatched
            with contextlib.suppress(Exception):
                self.delete(experiment_id)
            raise

        return experiment_id

    def follow(self, experiment_id: str) -> RemoteStatus:
        """Polls until the experiment ends, riding out a server or network that fails for up to `patience` seconds."""
        answered = time.monotonic()
        while True:
            try:
                status = self.status(experiment_id)
                answered = time.monotonic()
                if status.state.terminal:
                    return status
            except (ApiFailure, OSError, http.client.HTTPException) as failure:
                if not transient(failure) or time.monotonic() - answered > self.patience:
                    raise

            time.sleep(self.poll_interval)

    def results(self, experiment_id: str, scenarios: list[KotlinObject]) -> SimulationResults:
        """Downloads the tables of the runs of the experiment that succeeded, with the `scenarios` it ran."""
        with tempfile.TemporaryDirectory(prefix="opendc-") as directory:
            root = Path(directory)
            self.download_archive(experiment_id, root / "archive.zip")
            unpack(root / "archive.zip", root / "archive")

            outputs = list((root / "archive").glob("*/raw-output"))
            if len(outputs) != 1:
                raise ValueError(f"The archive holds {len(outputs)} raw-output directories rather than one")
            tables = read_tables(outputs[0])

        return SimulationResults(tables, scenario_frame(scenarios))

    def project_named(self, name: str) -> str:
        """Returns the id of the caller's project called `name`, created if they have none."""
        projects = send(self.request("GET", "api/v1/projects?filter=own"))
        ids = [project["id"] for project in projects if project["name"] == name]
        if ids:
            return ids[0]

        return send(with_document(self.request("POST", "api/v1/projects"), {"name": name}))["id"]

    def create_experiment(self, project_id: str, name: str, spec: KotlinObject) -> str:
        """Creates `spec` as a draft named `name` in the project, and returns its id."""
        document = {"projectId": project_id, "name": name, "spec": json.loads(sdk.SdkJson.encode_to_string(spec))}
        return send(with_document(self.request("POST", "api/v1/experiments"), document))["id"]

    def submit(self, experiment_id: str) -> None:
        """Queues the draft to run."""
        send(self.request("POST", f"api/v1/experiments/{experiment_id}/submit"))

    def cancel(self, experiment_id: str) -> None:
        """Stops the runs of the experiment that have not ended."""
        send(self.request("POST", f"api/v1/experiments/{experiment_id}/cancel"))

    def delete(self, experiment_id: str) -> None:
        """Deletes the experiment and everything it produced."""
        send(self.request("DELETE", f"api/v1/experiments/{experiment_id}"))

    def status(self, experiment_id: str) -> RemoteStatus:
        """Reads the state and progress of the experiment, and why each of its failed scenarios failed."""
        status = send(self.request("GET", f"api/v1/experiments/{experiment_id}/status"))
        failures = [scenario_failure(scenario) for scenario in status["scenarios"] if scenario.get("exitInfo")]
        return RemoteStatus(RemoteState(status["state"]), status["completedTasks"], status["totalTasks"], failures)

    def download_archive(self, experiment_id: str, target: Path) -> None:
        """Writes the zip of every file the runs of the experiment that succeeded produced to `target`."""
        request = self.request("GET", f"api/v1/experiments/{experiment_id}/archive", accept=ZIP)
        with opened(request) as response, target.open("wb") as file:
            shutil.copyfileobj(response, file)

    def upload_trace(self, key: str, trace: Trace) -> str:
        """Returns the library slug of `trace`, uploaded under `key` and its digest unless the library holds it."""
        check_traces({key: trace})

        with tempfile.TemporaryDirectory(prefix="opendc-") as directory:
            tables = {file.stem: file for file in trace.write_tables(Path(directory))}
            name = trace_name(key, tables.values())
            held = self.owned_slugs(trace.kind, name)
            if held:
                return held[0]

            # Registering takes the name over from an unfinished upload, so an upload cut short is sent again
            sizes = [{"table": table, "sizeBytes": file.stat().st_size} for table, file in tables.items()]
            registration = {"kind": trace.kind, "name": name, "files": sizes}
            registered = send(with_document(self.request("POST", "api/v1/traces"), registration))
            for slot in registered["uploads"]:
                self.put(tables[slot["table"]], slot)
            send(self.request("POST", f"api/v1/traces/{registered['trace']['id']}/complete"))

        return registered["trace"]["slug"]

    def owned_slugs(self, kind: str, name: str) -> list[str]:
        """Reads the slugs of the caller's finished traces of `kind` called `name`: one at most."""
        library = send(self.request("GET", f"api/v1/traces?kind={kind}"))
        owned = [found["slug"] for found in library if found["access"] == "owned"]
        return [slug for slug in owned if slug.endswith(f"/{name}")]

    def put(self, file: Path, slot: Mapping[str, Any]) -> None:
        """Sends `file` where the upload `slot` says: in parts to storage at signed URLs, or whole to the server."""
        if slot["direct"]:
            put_parts(file, slot["parts"])
        else:
            # The server takes a table whole, in the one part it hands out
            [part] = slot["parts"]
            self.put_whole(file, part["url"])

    def put_whole(self, file: Path, path: str) -> None:
        """Streams `file` to `path` on the server."""
        with file.open("rb") as content:
            opened(with_content(self.request("PUT", path), content, file.stat().st_size)).close()

    def request(self, method: str, path: str, *, accept: str = JSON) -> urllib.request.Request:
        """A request without a body for `path` on the server, with the credentials."""
        url = urllib.parse.urljoin(f"{self.url}/", path)
        request = urllib.request.Request(url, method=method, headers={"Accept": accept, "User-Agent": USER_AGENT})

        # urllib copies ordinary headers onto a redirect, which may lead to another host
        for name, value in self.credentials.items():
            request.add_unredirected_header(name, value)
        return request


def put_parts(file: Path, parts: list[Mapping[str, Any]]) -> None:
    """Sends each part of `file` to storage at its signed URL, reading only that part's bytes."""
    with file.open("rb") as content:
        for part in parts:
            content.seek(part["offset"])
            # A signed URL accepts no other credentials
            request = urllib.request.Request(part["url"], method="PUT", headers={"User-Agent": USER_AGENT})
            opened(with_content(request, content.read(part["length"]), part["length"])).close()


def with_content(request: urllib.request.Request, content: bytes | BinaryIO, length: int) -> urllib.request.Request:
    """`request`, carrying the `length` bytes of `content` as its binary body."""
    # Setting the body drops the length, so the length comes after; without one, urllib sends a file chunked
    request.data = content
    request.add_header("Content-Type", OCTET_STREAM)
    request.add_header("Content-Length", str(length))
    return request


def with_document(request: urllib.request.Request, document: Any) -> urllib.request.Request:
    """`request`, carrying `document` as its JSON body."""
    request.data = json.dumps(document).encode()
    request.add_header("Content-Type", JSON)
    return request


def send(request: urllib.request.Request) -> Any:
    """The JSON document the server answers `request` with, or `{}` for an empty answer."""
    with opened(request) as response:
        body = response.read()
    return json.loads(body) if body.strip() else {}


def opened(request: urllib.request.Request) -> http.client.HTTPResponse:
    """The answer to `request`, which the caller closes; an HTTP error raises an `ApiFailure`."""
    try:
        return urllib.request.urlopen(request, timeout=TIMEOUT)
    except urllib.error.HTTPError as error:
        raise failure_of(error) from None


def failure_of(error: urllib.error.HTTPError) -> ApiFailure:
    """The problem document the server answered with, or the bare HTTP status when it sent none."""
    with error:
        body = error.read()
    try:
        problem = json.loads(body)
    except ValueError:
        problem = {}
    if not isinstance(problem, dict):
        problem = {}

    issues = [
        f"{issue['path']}: {issue['message']}" if issue.get("path") else issue.get("message", "")
        for issue in problem.get("issues", [])
    ]
    return ApiFailure(error.code, problem.get("title", f"The server answered HTTP {error.code}"), issues)


def transient(failure: Exception) -> bool:
    """Whether `failure` may well pass: the network's, or the server erring rather than refusing."""
    return not isinstance(failure, ApiFailure) or failure.status >= SERVER_ERROR


def scenario_failure(scenario: Mapping[str, Any]) -> str:
    """Why a scenario failed, from its status: `scenario <index>: <reason> (<message>)`."""
    exit_info = scenario["exitInfo"]
    message = exit_info.get("message", "")
    return f"scenario {scenario['scenarioIndex']}: {exit_info['reason']}" + (f" ({message})" if message else "")


def trace_name(key: str, files: Iterable[Path]) -> str:
    """The library name of a trace passed under `key`: `key` as far as a name allows, then a digest of its `files`."""
    digest = hashlib.sha256()
    for file in files:
        digest.update(file.name.encode())
        with file.open("rb") as content:
            digest.update(hashlib.file_digest(content, "sha256").digest())

    stem = re.sub(r"[^a-z0-9._]+", "-", key.lower()).strip("-._")[: TRACE_NAME_CAP - DIGEST_LENGTH - 1]
    return "-".join(part for part in (stem, digest.hexdigest()[:DIGEST_LENGTH]) if part)


def unpack(archive: Path, root: Path) -> None:
    """Writes the files of the zip `archive` under `root`, refusing an archive that names a path outside it."""
    root = root.resolve()
    with zipfile.ZipFile(archive) as files:
        entries = [(entry, (root / entry.filename).resolve()) for entry in files.infolist() if not entry.is_dir()]
        outside = [entry.filename for entry, target in entries if not target.is_relative_to(root)]
        if outside:
            raise ValueError(f"The archive names paths outside {root}: {outside}")

        for entry, target in entries:
            target.parent.mkdir(parents=True, exist_ok=True)
            with files.open(entry) as source, target.open("wb") as file:
                shutil.copyfileobj(source, file)
