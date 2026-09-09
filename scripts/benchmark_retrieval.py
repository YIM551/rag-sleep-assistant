"""Offline analysis of 2026 RAG_STAGE_TIMING logs. Makes no network requests."""
import argparse
import json
import math
import re
import statistics
from pathlib import Path

STAGES = ("query_expansion_ms", "retrieval_ms", "reranking_ms", "prompt_build_ms", "llm_ms", "total_ms")


def analyze(lines):
    rows = []
    for number, line in enumerate(lines, 1):
        if "RAG_STAGE_TIMING" not in line:
            continue
        match = re.search(r"RAG_STAGE_TIMING\s+\{([^}]+)\}", line)
        if not match:
            raise ValueError(f"Malformed timing line {number}")
        pairs = [item.strip().split("=", 1) for item in match[1].split(",")]
        if any(len(pair) != 2 for pair in pairs) or len(pairs) != len(STAGES):
            raise ValueError(f"Invalid stage count on line {number}")
        row = {key: float(value) for key, value in pairs}
        if set(row) != set(STAGES) or any(not math.isfinite(v) or v < 0 for v in row.values()):
            raise ValueError(f"Invalid durations on line {number}")
        if any(row[key] > row["total_ms"] for key in STAGES):
            raise ValueError(f"Stage exceeds total on line {number}")
        rows.append(row)
    if not rows:
        raise ValueError("No successful-path RAG_STAGE_TIMING records found")
    metrics = {}
    for key in STAGES:
        values = sorted(row[key] for row in rows)
        metrics[key] = {"mean_ms": statistics.mean(values), "p95_ms": values[math.ceil(.95 * len(values)) - 1]}
    return {"successful_records": len(rows), "p95_method": "nearest-rank", "stages": metrics,
            "limitations": ["successful paths only; failures and guard exits are not emitted",
                            "input must contain a single configuration/run and already exclude warmup",
                            "query_expansion includes LLM rewriting and PRF; retrieval_ms excludes them",
                            "retrieval_ms includes per-query RRF/MMR, keyword filtering and deduplication",
                            "no concurrency, throughput, or semantic-quality conclusion"]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path)
    args = parser.parse_args()
    if args.log is None:
        print("DRY RUN: no requests or measurement. Supply --log from one controlled run; see docs/performance.md.")
        return
    try:
        report = analyze(args.log.read_text(encoding="utf-8-sig").splitlines())
    except (OSError, ValueError) as exc:
        parser.error(str(exc))
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

