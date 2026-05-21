#!/usr/bin/env python3
"""
Compare JMH benchmark results (with heapMetric) against a stored baseline and
print a markdown table suitable for a GitHub PR comment.

Usage:
    python compare_benchmarks.py <current_results.json> [<baseline_latest.json>]

If the baseline file is absent or empty the table shows current values only.
"""

import json
import sys
from pathlib import Path


def short_name(full: str) -> str:
    """Strip package prefix, keep ClassName.methodName."""
    parts = full.rsplit(".", 2)
    return ".".join(parts[-2:]) if len(parts) >= 2 else full


def fmt_delta(current: float, baseline: float) -> str:
    if baseline == 0:
        return "-"
    pct = (current - baseline) / baseline * 100
    return f"{pct:+.1f}%"


def fmt_float(value: float | None, decimals: int = 1) -> str:
    if value is None:
        return "N/A"
    return f"{value:,.{decimals}f}"


def load_results(path: Path) -> dict[str, dict]:
    """Return dict keyed by benchmark name.

    Accepts either a raw results array or a benchmark-history array, in which
    case the last entry's results are used as the baseline.
    """
    if not path.exists():
        return {}
    data = json.loads(path.read_text())
    if not isinstance(data, list) or not data:
        return {}
    # History format: list of {commit, timestamp, results: [...]}
    if "results" in data[-1]:
        data = data[-1]["results"]
    return {entry["benchmark"]: entry for entry in data if "benchmark" in entry}


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: compare_benchmarks.py <current.json> [baseline.json]", file=sys.stderr)
        sys.exit(1)

    current_path = Path(sys.argv[1])
    baseline_path = Path(sys.argv[2]) if len(sys.argv) > 2 else None

    current = load_results(current_path)
    baseline = load_results(baseline_path) if baseline_path else {}

    if not current:
        print("## Benchmark Results\n\n_No benchmark results found._")
        return

    has_baseline = bool(baseline)

    lines = ["## Benchmark Results\n"]
    if has_baseline:
        # Extract the baseline commit SHA if stored in the file (update_history stores it)
        first = next(iter(baseline.values()), {})
        sha = first.get("_meta_commit", "")
        lines.append(f"> Compared against master{f' @ `{sha[:7]}`' if sha else ''}\n")
    else:
        lines.append("> No baseline found — showing current values only.\n")

    header = (
        "| Benchmark | Time (ms) | Δ Time"
        " | Avg Heap (MB) | Δ Avg Heap"
        " | Max Heap (MB) | Δ Max Heap |"
    )
    separator = (
        "|-----------|----------:|-------:"
        "|--------------:|-----------:"
        "|--------------:|-----------:|"
    )
    lines += [header, separator]

    for name, entry in sorted(current.items(), key=lambda x: x[0]):
        score = entry.get("primaryMetric", {}).get("score")
        heap = entry.get("heapMetric", {})
        avg_mb = heap.get("avgMb")
        max_mb = heap.get("maxMb")

        base = baseline.get(name, {})
        base_score = base.get("primaryMetric", {}).get("score") if base else None
        base_heap = base.get("heapMetric", {}) if base else {}
        base_avg = base_heap.get("avgMb")
        base_max = base_heap.get("maxMb")

        delta_time = fmt_delta(score, base_score) if (has_baseline and score is not None and base_score is not None) else "-"
        delta_avg = fmt_delta(avg_mb, base_avg) if (has_baseline and avg_mb is not None and base_avg is not None) else "-"
        delta_max = fmt_delta(max_mb, base_max) if (has_baseline and max_mb is not None and base_max is not None) else "-"

        lines.append(
            f"| {short_name(name)}"
            f" | {fmt_float(score)}"
            f" | {delta_time}"
            f" | {fmt_float(avg_mb)}"
            f" | {delta_avg}"
            f" | {fmt_float(max_mb)}"
            f" | {delta_max} |"
        )

    lines.append("\n_Positive Δ = slower / more memory. No threshold gate — informational only._")
    print("\n".join(lines))


if __name__ == "__main__":
    main()