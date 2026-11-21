#!/bin/bash
# Runs one OpenDC launcher as a SLURM batch job and records how it ended.
# Usage: wrapper.sh <launcher-dir> <java> <heap-mb> <main-class>
#
# The cluster keeps no accounting a dispatcher could ask afterwards, so this script is the observer:
# it records when the job started, whether SIGTERM arrived, and the launcher's exit code with the
# kernel's out-of-memory count and peak memory, read only from this job's own cgroup.
launcher_dir=$1
java=$2
heap_mb=$3
main_class=$4
cgroup_root=${OPENDC_CGROUP_ROOT:-/sys/fs/cgroup}
self_cgroup=${OPENDC_SELF_CGROUP:-/proc/self/cgroup}

record() {
  printf '%s\n' "$2" > ".$1.tmp" && mv -f ".$1.tmp" "$1"
}

job_cgroup() {
  local line path
  [ -n "$SLURM_JOB_ID" ] || return 1
  while IFS= read -r line; do
    case $line in
      0::*) path=${line#0::} ;;
      *:memory:*|*:memory,*|*,memory:*|*,memory,*) path=memory${line##*:} ;;
      *) continue ;;
    esac
    case $path in
      */job_"$SLURM_JOB_ID"|*/job_"$SLURM_JOB_ID"/*) printf '%s\n' "$cgroup_root/${path#/}"; return 0 ;;
    esac
  done < "$self_cgroup"
  return 1
}

readings() {
  local dir=$1 events key value
  if [ -f "$dir/memory.peak" ]; then
    printf 'memory_peak=%s\n' "$(< "$dir/memory.peak")"
  elif [ -f "$dir/memory.max_usage_in_bytes" ]; then
    printf 'memory_peak=%s\n' "$(< "$dir/memory.max_usage_in_bytes")"
  fi
  for events in "$dir/memory.events" "$dir/memory.oom_control"; do
    [ -f "$events" ] || continue
    while read -r key value; do
      [ "$key" = oom_kill ] && printf 'oom_kill=%s\n' "$value"
    done < "$events"
  done
}

record started "at=$(date +%s)"
child=
trap 'touch terminated; [ -n "$child" ] && kill -TERM "$child" 2>/dev/null' TERM

MANIFEST_URL="file://$PWD/manifest.json" "$java" "-Xmx${heap_mb}m" -XX:+ExitOnOutOfMemoryError \
  -cp "$launcher_dir/lib/*" "$main_class" &
child=$!
wait "$child"
code=$?
while kill -0 "$child" 2>/dev/null; do
  wait "$child"
  code=$?
done

lines="code=$code
ended=$(date +%s)"
if dir=$(job_cgroup) && [ -d "$dir" ]; then
  lines="$lines
$(readings "$dir")"
fi
record exit "$lines"
exit "$code"
