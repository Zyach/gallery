#!/usr/bin/env bash
set -euo pipefail

RUN_ID=${1:-$(date +%Y%m%d-%H%M%S)}
AUTO_STOP_SECONDS=${2:-${AUTO_STOP_SECONDS:-0}}
RESET_SHARED_METRICS=${RESET_SHARED_METRICS:-1}
TERMUX_CMD=${TERMUX_CMD:-$HOME/.local/bin/termux-cmd.sh}
TARGET_PKG=${TARGET_PKG:-com.google.aiedge.gallery}
RUN_DIR="/sdcard/Download/edgegallery/runs/$RUN_ID"
HOST_RUN_DIR=${HOST_RUN_DIR:-$HOME/storage/downloads/edgegallery/runs/$RUN_ID}

RISH_BIN="/data/data/com.termux/files/usr/bin/rish"
RUN_METRICS_BIN="/data/data/com.termux/files/home/.local/bin/run_metrics_full"

echo "== EdgeGallery diagnóstico: $RUN_ID =="

"$TERMUX_CMD" "mkdir -p $RUN_DIR"
mkdir -p "$HOST_RUN_DIR"

if [ "$RESET_SHARED_METRICS" -eq 1 ]; then
  "$TERMUX_CMD" "rm -f /sdcard/Download/edgegallery/metrics_live.txt /sdcard/Download/edgegallery/metrics_gfx.txt /sdcard/Download/edgegallery/metrics_smaps.txt /sdcard/Download/edgegallery/captura_metrics.log /sdcard/Download/edgegallery/launch.log"
fi

"$TERMUX_CMD" "sh $RISH_BIN -c 'logcat -c'"

"$TERMUX_CMD" "sh $RISH_BIN -c 'nohup logcat -v epoch -s LlmHttpService AGLlmChatModelHelper AGMainActivity > $RUN_DIR/logcat_llm.txt 2>&1 & echo \$! > $RUN_DIR/logcat_pid.txt'"

"$TERMUX_CMD" "RISH_PATH=$RISH_BIN LOG_FILE=$RUN_DIR/captura_metrics.log LAUNCH_LOG=$RUN_DIR/launch.log PID_FILE=$RUN_DIR/metrics_pid.txt EXTRA_GFX=1 EXTRA_SMAPS=1 HEAVY_INTERVAL=300 PERFETTO=1 PERFETTO_DUR=20 PERFETTO_OUT=$RUN_DIR/perfetto_trace_start.pftrace PERFETTO_LATE_OUT=$RUN_DIR/perfetto_trace_late.pftrace PERFETTO_LATE_DELAY=900 $RUN_METRICS_BIN 60"

"$TERMUX_CMD" "sh $RISH_BIN -c 'am start -n com.google.aiedge.gallery/com.google.ai.edge.gallery.MainActivity'"

echo "Ejecuta el escenario en el telefono y pulsa Enter para cerrar la captura."
if [ "$AUTO_STOP_SECONDS" -gt 0 ]; then
  echo "Auto-stop en $AUTO_STOP_SECONDS segundos..."
  sleep "$AUTO_STOP_SECONDS"
else
  read -r
fi

"$TERMUX_CMD" "if [ -f $RUN_DIR/metrics_pid.txt ]; then kill \$(cat $RUN_DIR/metrics_pid.txt) 2>/dev/null || true; fi"
"$TERMUX_CMD" "if [ -f $RUN_DIR/logcat_pid.txt ]; then kill \$(cat $RUN_DIR/logcat_pid.txt) 2>/dev/null || true; fi"

"$TERMUX_CMD" "cp -f /sdcard/Download/edgegallery/metrics_live.txt $RUN_DIR/metrics_live.txt 2>/dev/null || true"
"$TERMUX_CMD" "cp -f /sdcard/Download/edgegallery/metrics_gfx.txt $RUN_DIR/metrics_gfx.txt 2>/dev/null || true"
"$TERMUX_CMD" "cp -f /sdcard/Download/edgegallery/metrics_smaps.txt $RUN_DIR/metrics_smaps.txt 2>/dev/null || true"

"$TERMUX_CMD" "sh $RISH_BIN -c 'dumpsys meminfo $TARGET_PKG'" > "$HOST_RUN_DIR/meminfo_end.txt" || true
"$TERMUX_CMD" "sh $RISH_BIN -c 'dumpsys gfxinfo $TARGET_PKG'" > "$HOST_RUN_DIR/gfxinfo_end.txt" || true

echo "Logs guardados en: $HOST_RUN_DIR"

ANALYZER="$(cd "$(dirname "$0")" && pwd)/edgegallery_analyze.py"
if [ -f "$ANALYZER" ]; then
  python3 "$ANALYZER" "$HOST_RUN_DIR" || true
fi
