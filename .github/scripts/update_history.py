#!/usr/bin/env python3
"""
Append the current JMH results to the benchmark history and write the updated
history to a new file.

Usage:
    python update_history.py <results.json> <existing-history.json> <output.json> <commit_sha>
"""

import json
import sys
from datetime import datetime, timezone
from pathlib import Path


def main() -> None:
    if len(sys.argv) < 5:
        print(
            "Usage: update_history.py <results.json> <existing-history.json> <output.json> <commit_sha>",
            file=sys.stderr,
        )
        sys.exit(1)

    results_path = Path(sys.argv[1])
    existing_path = Path(sys.argv[2])
    output_path = Path(sys.argv[3])
    commit_sha = sys.argv[4]

    results = json.loads(results_path.read_text())

    for entry in results:
        entry["_meta_commit"] = commit_sha

    history = json.loads(existing_path.read_text()) if existing_path.exists() else []

    history.append(
        {
            "commit": commit_sha,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "results": results,
        }
    )

    output_path.write_text(json.dumps(history, indent=2))
    print(f"Stored results for commit {commit_sha[:7]} ({len(results)} benchmark(s)).")


if __name__ == "__main__":
    main()