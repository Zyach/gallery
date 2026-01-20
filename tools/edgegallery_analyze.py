import json
import re
import statistics
import sys
from pathlib import Path


def read_text(path: Path) -> str:
    try:
        return path.read_text(errors="ignore")
    except Exception:
        return ""


def last_match(text: str, pattern: str):
    matches = re.findall(pattern, text, flags=re.MULTILINE)
    if not matches:
        return None
    return matches[-1]


def parse_gfx_metrics(text: str):
    metrics = {}
    patterns = {
        "p90_ms": r"90th percentile: (\d+)ms",
        "p95_ms": r"95th percentile: (\d+)ms",
        "p99_ms": r"99th percentile: (\d+)ms",
        "missed_vsync": r"Number Missed Vsync: (\d+)",
        "high_input_latency": r"Number High input latency: (\d+)",
        "slow_ui_thread": r"Number Slow UI thread: (\d+)",
        "slow_bitmap": r"Number Slow bitmap uploads: (\d+)",
        "slow_issue_draw": r"Number Slow issue draw commands: (\d+)",
        "frame_deadline_missed": r"Number Frame deadline missed: (\d+)",
        "frame_deadline_missed_legacy": r"Number Frame deadline missed \(legacy\): (\d+)",
        "gpu_p50_ms": r"50th gpu percentile: (\d+)ms",
        "gpu_p90_ms": r"90th gpu percentile: (\d+)ms",
        "gpu_p95_ms": r"95th gpu percentile: (\d+)ms",
        "gpu_p99_ms": r"99th gpu percentile: (\d+)ms",
    }
    for key, pattern in patterns.items():
        value = last_match(text, pattern)
        if value is not None:
            try:
                metrics[key] = int(value)
            except ValueError:
                pass
    return metrics


def parse_meminfo(text: str):
    metrics = {}
    patterns = {
        "MemAvailable_kb": r"MemAvailable:\s+(\d+) kB",
        "MemFree_kb": r"MemFree:\s+(\d+) kB",
        "Cached_kb": r"Cached:\s+(\d+) kB",
        "SwapFree_kb": r"SwapFree:\s+(\d+) kB",
        "SwapTotal_kb": r"SwapTotal:\s+(\d+) kB",
        "AnonPages_kb": r"AnonPages:\s+(\d+) kB",
        "PageTables_kb": r"PageTables:\s+(\d+) kB",
    }
    for key, pattern in patterns.items():
        value = last_match(text, pattern)
        if value is not None:
            try:
                metrics[key] = int(value)
            except ValueError:
                pass
    return metrics


def percentile(values, pct):
    if not values:
        return None
    values_sorted = sorted(values)
    k = (len(values_sorted) - 1) * (pct / 100.0)
    f = int(k)
    c = min(f + 1, len(values_sorted) - 1)
    if f == c:
        return values_sorted[f]
    return values_sorted[f] + (values_sorted[c] - values_sorted[f]) * (k - f)


def parse_llm_events(text: str):
    totals = []
    ttfb = []
    counts = {
        "request_start": 0,
        "request_done": 0,
        "request_error": 0,
        "request_cancel": 0,
        "request_rejected": 0,
        "request_empty": 0,
    }
    for line in text.splitlines():
        if "LLM_HTTP" not in line:
            continue
        if "request_start" in line:
            counts["request_start"] += 1
        if "request_done" in line:
            counts["request_done"] += 1
        if "request_error" in line:
            counts["request_error"] += 1
        if "request_cancel" in line:
            counts["request_cancel"] += 1
        if "request_rejected" in line:
            counts["request_rejected"] += 1
        if "request_empty" in line:
            counts["request_empty"] += 1
        m_total = re.search(r"totalMs=(\d+)", line)
        if m_total:
            totals.append(int(m_total.group(1)))
        m_ttfb = re.search(r"ttfbMs=(\d+)", line)
        if m_ttfb:
            ttfb_val = int(m_ttfb.group(1))
            if ttfb_val >= 0:
                ttfb.append(ttfb_val)
    stats = {
        "counts": counts,
        "totalMs_p50": percentile(totals, 50),
        "totalMs_p95": percentile(totals, 95),
        "totalMs_mean": statistics.mean(totals) if totals else None,
        "ttfbMs_p50": percentile(ttfb, 50),
        "ttfbMs_p95": percentile(ttfb, 95),
        "ttfbMs_mean": statistics.mean(ttfb) if ttfb else None,
    }
    return stats


def main():
    if len(sys.argv) < 2:
        print("Uso: edgegallery_analyze.py <run_dir>")
        return 1

    run_dir = Path(sys.argv[1])
    if not run_dir.exists():
        print(f"No existe: {run_dir}")
        return 1

    gfx_text = read_text(run_dir / "metrics_gfx.txt")
    if not gfx_text:
        gfx_text = read_text(run_dir / "captura_metrics.log")
    if not gfx_text:
        gfx_text = read_text(run_dir / "gfxinfo_end.txt")

    mem_text = read_text(run_dir / "metrics_live.txt")
    if not mem_text:
        mem_text = read_text(run_dir / "meminfo_end.txt")

    logcat_text = read_text(run_dir / "logcat_llm.txt")
    llm_stats = parse_llm_events(logcat_text)

    summary = {
        "run_dir": str(run_dir),
        "gfx": parse_gfx_metrics(gfx_text),
        "memory": parse_meminfo(mem_text),
        "llm_http": llm_stats,
    }

    summary_path = run_dir / "summary.json"
    try:
        summary_path.write_text(json.dumps(summary, indent=2))
    except Exception:
        pass

    print("== Resumen diagnostico ==")
    gfx = summary["gfx"]
    if gfx:
        print(
            f"Jank p90/p95/p99: {gfx.get('p90_ms','?')} / {gfx.get('p95_ms','?')} / {gfx.get('p99_ms','?')} ms"
        )
        print(
            f"Missed vsync: {gfx.get('missed_vsync','?')}, Slow UI: {gfx.get('slow_ui_thread','?')}, Frame deadline: {gfx.get('frame_deadline_missed','?')}"
        )
        print(
            f"GPU p50/p95/p99: {gfx.get('gpu_p50_ms','?')} / {gfx.get('gpu_p95_ms','?')} / {gfx.get('gpu_p99_ms','?')} ms"
        )
    else:
        print("Jank: sin datos")

    mem = summary["memory"]
    if mem:
        print(
            f"MemAvailable: {mem.get('MemAvailable_kb','?')} kB, SwapFree: {mem.get('SwapFree_kb','?')} kB, Cached: {mem.get('Cached_kb','?')} kB"
        )
    else:
        print("Memoria: sin datos")

    counts = llm_stats.get("counts", {})
    print(
        "LLM HTTP: start={start} done={done} error={error} cancel={cancel} rejected={rejected}".format(
            start=counts.get("request_start", 0),
            done=counts.get("request_done", 0),
            error=counts.get("request_error", 0),
            cancel=counts.get("request_cancel", 0),
            rejected=counts.get("request_rejected", 0),
        )
    )
    if llm_stats.get("totalMs_p50") is not None:
        print(
            f"LLM lat total p50/p95: {llm_stats['totalMs_p50']:.0f} / {llm_stats['totalMs_p95']:.0f} ms"
        )
    if llm_stats.get("ttfbMs_p50") is not None:
        print(
            f"LLM TTFB p50/p95: {llm_stats['ttfbMs_p50']:.0f} / {llm_stats['ttfbMs_p95']:.0f} ms"
        )
    print(f"Summary JSON: {summary_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
