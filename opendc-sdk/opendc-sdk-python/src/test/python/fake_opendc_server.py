"""The slice of the OpenDC API that `opendc.Server` talks to, on loopback, like the CLI's `FakeOpendcServer.kt`."""

import io
import json
import re
import signal
import threading
import zipfile
from collections.abc import Iterable, Mapping
from dataclasses import dataclass, field
from email.message import Message
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Literal, cast
from urllib.parse import parse_qs, urlsplit

import pyarrow as pa
import pyarrow.parquet as pq

HANDLE = "alice"
"""The handle of the caller, which their traces are named under."""

TOTAL_TASKS = 4

STORAGE_PART_SIZE = 1000
"""How many bytes each part of an upload to storage takes."""

TABLES = {"workload": ["tasks", "fragments"], "carbon": ["carbon"], "failure": ["failures"]}

FAILED_SCENARIO = {"exitCode": 23, "reason": "simulationError", "message": "threw"}

Answer = tuple[int, bytes, str]
"""The status, body and content type of an answer."""

NO_CONTENT: Answer = (204, b"", "text/plain")


class Dropped(Exception):
    """The connection is closed without an answer."""


@dataclass(frozen=True)
class Recorded:
    """A request the fake answered."""

    method: str
    path: str
    headers: Message
    body: bytes


@dataclass
class StoredTrace:
    """A trace in the library, with the bytes of each of its tables that have arrived, by offset."""

    id: str
    slug: str
    kind: str
    sizes: dict[str, int]
    received: dict[str, dict[int, bytes]] = field(default_factory=dict)
    complete: bool = False

    def contents(self, table: str) -> bytes:
        return b"".join(part for _, part in sorted(self.received.get(table, {}).items()))

    def wire(self) -> dict[str, Any]:
        tables = [{"name": table, "sizeBytes": size} for table, size in self.sizes.items()]
        return {"id": self.id, "slug": self.slug, "kind": self.kind, "access": "owned", "tables": tables}


def host_table() -> bytes:
    """A host table as a run exports one."""
    table = pa.table({"timestamp": [60_000, 120_000], "host_id": ["H01", "H01"], "cpu_usage": [1000.0, 500.0]})
    buffer = io.BytesIO()
    pq.write_table(table, buffer)
    return buffer.getvalue()


RESULTS = {
    "tiny/raw-output/0/seed=0/host.parquet": host_table(),
    "tiny/raw-output/0/seed=0/meta/trackr.json": b"{}",
}
"""The files of the archive of a run of scenario 0 with seed 0, laid out as the server lays them out."""


def zipped(files: Mapping[str, bytes]) -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content)
    return buffer.getvalue()


def problem(status: int, title: str, issues: Iterable[tuple[str, str]] = ()) -> Answer:
    listed = [{"path": path, "message": message} for path, message in issues]
    return document(status, {"status": status, "title": title, "issues": listed})


def document(status: int, body: Any) -> Answer:
    return status, json.dumps(body).encode(), "application/json"


class FakeOpendcServer(ThreadingHTTPServer):
    """A fake OpenDC server: each status read answers the next of `statuses`, and the last one from then on."""

    def __init__(
        self,
        *,
        statuses: Iterable[str] = ("running", "running", "succeeded"),
        shared_projects: Iterable[str] = (),
        submit_issues: Iterable[tuple[str, str]] = (),
        drops_submit: bool = False,
        archive: Mapping[str, bytes] = RESULTS,
        uploads: Literal["server", "storage"] = "server",
        failing_puts: int = 0,
    ) -> None:
        super().__init__(("127.0.0.1", 0), Handler)
        # A step is an experiment state, or "unavailable", "dropped" or "interrupt" (Ctrl-C while it runs)
        self.statuses = list(statuses)
        self.shared_projects = list(shared_projects)
        self.submit_issues = list(submit_issues)
        self.drops_submit = drops_submit
        self.archive = zipped(archive)
        self.uploads = uploads
        self.failing_puts = failing_puts

        self.requests: list[Recorded] = []
        self.projects: list[str] = []
        self.experiments: dict[str, Any] = {}
        self.deleted: list[str] = []
        self.cancelled: list[str] = []
        self.registrations: list[str] = []
        self.traces: dict[str, StoredTrace] = {}
        self.status_reads = 0
        self.lock = threading.Lock()

        threading.Thread(target=self.serve_forever, daemon=True).start()

    @property
    def url(self) -> str:
        return f"http://127.0.0.1:{self.server_port}"

    def server_close(self) -> None:
        self.shutdown()
        super().server_close()

    def paths(self, method: str) -> list[str]:
        """The paths of the requests of `method`, in the order they came."""
        return [request.path for request in self.requests if request.method == method]

    def library(self) -> dict[str, StoredTrace]:
        """The complete traces, by slug."""
        return {trace.slug: trace for trace in self.traces.values() if trace.complete}

    def answer(self, request: Recorded, query: dict[str, list[str]]) -> Answer:
        with self.lock:
            self.requests.append(request)
            # The real server matches a bodyless request that declares a content type against what it consumes
            if request.method == "POST" and not request.body and request.headers.get("Content-Type"):
                return problem(415, "The content type of a request without a body is unsupported")
            return self.route(request, query)

    def route(self, request: Recorded, query: dict[str, list[str]]) -> Answer:
        method, path = request.method, request.path
        experiment = re.fullmatch(r"/api/v1/experiments/([^/]+)(?:/(\w+))?", path)
        trace = re.fullmatch(r"/api/v1/traces/([^/]+)/(complete|tables/(\w+))", path)
        stored = re.fullmatch(r"/storage/([^/]+)/(\w+)/(\d+)", path)

        if path == "/api/v1/projects" and method == "GET":
            return self.project_list(query.get("filter", ["all"])[0])
        if path == "/api/v1/projects" and method == "POST":
            name = json.loads(request.body)["name"]
            self.projects.append(name)
            return document(201, {"id": f"p-{name}", "name": name})

        if path == "/api/v1/experiments" and method == "POST":
            experiment_id = f"x-{len(self.experiments) + 1}"
            self.experiments[experiment_id] = json.loads(request.body)
            return document(201, {"id": experiment_id, "state": "draft"})
        if experiment and experiment[1] in self.experiments:
            return self.experiment(method, experiment[1], experiment[2] or "", request)

        if path == "/api/v1/traces" and method == "GET":
            kind = query["kind"][0]
            return document(200, [found.wire() for found in self.library().values() if found.kind == kind])
        if path == "/api/v1/traces" and method == "POST":
            return self.register(json.loads(request.body))
        if trace and trace[1] in self.traces and method == "POST" and trace[2] == "complete":
            return self.complete(self.traces[trace[1]])
        if trace and trace[1] in self.traces and method == "PUT":
            return self.receive(self.traces[trace[1]], trace[3], 0, request.body)

        if stored and stored[1] in self.traces and method == "PUT":
            # Storage refuses a second way of signing in next to the signature in the URL
            if request.headers.get("Authorization"):
                return problem(400, "Only one auth mechanism allowed")
            return self.receive(self.traces[stored[1]], stored[2], int(stored[3]), request.body)

        return problem(404, "Not found")

    def project_list(self, membership: str) -> Answer:
        own = [{"id": f"p-{name}", "name": name} for name in self.projects]
        # Listed first: the server orders projects by when they last changed, and a colleague may just have run theirs
        shared = [{"id": f"p-shared-{name}", "name": name} for name in self.shared_projects]
        return document(200, own if membership == "own" else shared + own)

    def experiment(self, method: str, experiment_id: str, action: str, request: Recorded) -> Answer:
        if method == "DELETE" and action == "":
            self.deleted.append(experiment_id)
            return NO_CONTENT
        if method == "POST" and action == "submit":
            if self.submit_issues:
                return problem(400, "The experiment document is invalid", self.submit_issues)
            if self.drops_submit:
                raise Dropped()
            return document(200, {"id": experiment_id, "state": "queued"})
        if method == "POST" and action == "cancel":
            self.cancelled.append(experiment_id)
            return document(200, {"id": experiment_id, "state": "cancelled"})
        if method == "GET" and action == "status":
            return self.status(experiment_id)
        if method == "GET" and action == "archive":
            if "application/zip" not in request.headers.get("Accept", ""):
                return problem(406, "The accept header value did not match")
            return 200, self.archive, "application/zip"
        return problem(404, "Not found")

    def status(self, experiment_id: str) -> Answer:
        step = self.statuses[min(self.status_reads, len(self.statuses) - 1)]
        self.status_reads += 1

        if step == "unavailable":
            return problem(503, "Service unavailable")
        if step == "dropped":
            raise Dropped()
        if step == "interrupt":
            # Through the process's handler, as Ctrl-C arrives; os.kill would end the process on Windows
            signal.raise_signal(signal.SIGINT)
            step = "running"

        scenario = {"scenarioIndex": 0, "state": step, "completedTasks": 0, "totalTasks": TOTAL_TASKS, "attempt": 1}
        if step in ("partial", "failed", "cancelled"):
            scenario["exitInfo"] = FAILED_SCENARIO

        done = TOTAL_TASKS if step == "succeeded" else 0
        status = {"id": experiment_id, "state": step, "completedTasks": done, "totalTasks": TOTAL_TASKS}
        return document(200, {**status, "scenarioCount": 1, "scenarios": [scenario]})

    def register(self, registration: dict[str, Any]) -> Answer:
        name, kind = registration["name"], registration["kind"]
        if not re.fullmatch(r"[a-z0-9][a-z0-9._-]{0,99}", name):
            return problem(400, "That is not a usable trace name", [("name", "use lower-case letters and digits")])

        sizes = {file["table"]: file["sizeBytes"] for file in registration["files"]}
        if sorted(sizes) != sorted(TABLES[kind]) or min(sizes.values()) <= 0:
            return problem(400, "Every file has to be accounted for before any of it is sent")

        # As the real server does: an unfinished trace of the name is taken over, a finished one is a conflict
        slug = f"{HANDLE}/{name}"
        existing = [found for found in self.traces.values() if found.slug == slug]
        if any(found.complete for found in existing):
            return problem(409, f"You already have a trace called {slug}")
        for found in existing:
            del self.traces[found.id]

        self.registrations.append(slug)
        trace = StoredTrace(f"t-{len(self.registrations)}", slug, kind, sizes)
        self.traces[trace.id] = trace

        return document(201, {"trace": trace.wire(), "uploads": [self.slot(trace, table) for table in TABLES[kind]]})

    def slot(self, trace: StoredTrace, table: str) -> dict[str, Any]:
        size = trace.sizes[table]
        if self.uploads == "server":
            parts = [{"url": f"api/v1/traces/{trace.id}/tables/{table}", "offset": 0, "length": size}]
            return {"table": table, "parts": parts, "direct": False}

        parts = [
            {
                "url": f"{self.url}/storage/{trace.id}/{table}/{offset}",
                "offset": offset,
                "length": min(STORAGE_PART_SIZE, size - offset),
            }
            for offset in range(0, size, STORAGE_PART_SIZE)
        ]
        return {"table": table, "parts": parts, "direct": True}

    def receive(self, trace: StoredTrace, table: str, offset: int, body: bytes) -> Answer:
        if self.failing_puts > 0:
            self.failing_puts -= 1
            return problem(500, "The store is unavailable")

        trace.received.setdefault(table, {})[offset] = body
        return NO_CONTENT

    def complete(self, trace: StoredTrace) -> Answer:
        missing = [table for table, size in trace.sizes.items() if len(trace.contents(table)) != size]
        if missing:
            return problem(409, f"The bytes for {', '.join(missing)} never arrived")

        trace.complete = True
        return document(200, trace.wire())


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        self.handle_request()

    def do_POST(self) -> None:
        self.handle_request()

    def do_PUT(self) -> None:
        self.handle_request()

    def do_DELETE(self) -> None:
        self.handle_request()

    def handle_request(self) -> None:
        url = urlsplit(self.path)
        length = int(self.headers["Content-Length"] or 0)
        request = Recorded(self.command, url.path, self.headers, self.rfile.read(length))
        try:
            status, answer, content_type = cast(FakeOpendcServer, self.server).answer(request, parse_qs(url.query))
        except Dropped:
            self.close_connection = True
            return

        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(answer)))
        self.end_headers()
        self.wfile.write(answer)

    def log_message(self, format: str, *args: Any) -> None:
        pass
