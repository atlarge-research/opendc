"""The example in the README runs and plots the power draw of each of its scenarios."""

import re
from pathlib import Path

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pytest

README = Path(__file__).parents[3] / "README.md"


def example_code() -> str:
    """The Python code blocks of the README's Example section, joined in order."""
    section = README.read_text(encoding="utf-8").split("\n## Example\n", 1)[1].split("\n## ", 1)[0]
    return "\n".join(re.findall(r"```python\n(.*?)```", section, re.DOTALL))


def test_example_plots_one_line_per_scenario(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.chdir(tmp_path)
    matplotlib.use("Agg")
    plt.close("all")
    code = compile(example_code(), str(README), "exec")

    exec(code, {})

    assert (tmp_path / "power.png").stat().st_size > 0
    lines = plt.gcf().axes[0].get_lines()
    assert [line.get_label() for line in lines] == ["4 hosts", "16 hosts"]
    small, large = (np.nanmean(np.asarray(line.get_ydata(), dtype=float)) for line in lines)
    # The README states that 16 hosts draw about three times the power of 4 hosts
    assert 2.5 < large / small < 3.5
    plt.close("all")
