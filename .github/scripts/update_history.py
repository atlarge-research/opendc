#!/usr/bin/env python3
"""
Append the current JMH results to benchmark-history.json and overwrite
benchmark-latest.json on the benchmark-data branch working directory.

Usage:
    python update_history.py <results.json> <output_dir> <commit_sha> [<pr_number>]

<output_dir> is the directory where benchmark-history.json and
benchmark-latest.json will be written (the benchmark-data checkout root).
"""

import json
import sys
from datetime import datetime, timezone
from pathlib import Path


def main() -> None:
    if len(sys.argv) < 4:
        print("Usage: update_history.py <results.json> <output_dir> <commit_sha> [<pr_number>]", file=sys.stderr)
        sys.exit(1)

    results_path = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    commit_sha = sys.argv[3]
    pr_number = int(sys.argv[4]) if len(sys.argv) > 4 else None

    results = json.loads(results_path.read_text())

    # Tag each result entry with the commit so compare_benchmarks.py can display it
    for entry in results:
        entry["_meta_commit"] = commit_sha

    history_path = output_dir / "benchmark-history.json"
    history = json.loads(history_path.read_text()) if history_path.exists() else []

    history.append(
        {
            "commit": commit_sha,
            "pr": pr_number,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "results": results,
        }
    )

    history_path.write_text(json.dumps(history, indent=2))

    print(f"Stored results for commit {commit_sha[:7]} ({len(results)} benchmark(s)).")


if __name__ == "__main__":
    main()