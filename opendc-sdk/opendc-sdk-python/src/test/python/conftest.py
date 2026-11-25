import tempfile
from collections.abc import Iterator
from pathlib import Path

import pytest

import opendc


def pytest_configure() -> None:
    # The suite runs a few tiny simulations, so a small JVM leaves room for Gradle next to it
    opendc.start_jvm("-Xmx1g")


@pytest.fixture
def scratch(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Iterator[Path]:
    """Puts the temporary files of a test in an empty directory, which the test must leave empty."""
    scratch = tmp_path / "scratch"
    scratch.mkdir()
    monkeypatch.setattr(tempfile, "tempdir", str(scratch))
    yield scratch
    assert list(scratch.iterdir()) == []
