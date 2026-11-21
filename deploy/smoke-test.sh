#!/bin/sh
# Checks a running OpenDC deployment end to end: the frontend is served from the API's origin, and an
# experiment over the built-in bitbrains-small trace is submitted, simulated and its results read
# back. Exits non-zero on the first thing that does not work.
#
#   OPENDC_URL=http://localhost:8080 deploy/smoke-test.sh
#
# Needs curl and jq, and a deployment in anonymous mode.
set -eu

url="${OPENDC_URL:-http://localhost:8080}"
api="$url/api/v1"
timeout="${OPENDC_SMOKE_TIMEOUT:-300}"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

step() {
    echo "--- $*"
}

step "server answers at $url"
curl -fsS "$api/config" | jq -e '.auth.type == "anonymous"' >/dev/null ||
    fail "the server is not up in anonymous mode"

step "a deep link finds its page"
curl -fsS "$url/project?id=00000000-0000-0000-0000-000000000000" | grep -qi '<html' ||
    fail "/project did not return the frontend page"

step "creating a project"
project=$(curl -fsS -X POST "$api/projects" -H 'Content-Type: application/json' \
    -d '{"name":"Smoke test"}' | jq -r '.id')

step "creating and submitting an experiment"
spec='{
    "name": "smoke",
    "topologies": [{"datacenters": [{"name": "DC 1", "clusters": [{"name": "Cluster 1", "hosts": [{
        "name": "Host", "count": 4,
        "cpu": {"coreCount": 16, "coreSpeed": "2.6 GHz"},
        "memory": {"size": "64 GiB"},
        "cpuPowerModel": {"type": "linear", "maxPower": "300 Watts", "idlePower": "90 Watts"}
    }]}]}]}],
    "workloads": [{"type": "trace", "source": {"type": "named", "name": "bitbrains-small"}}]
}'
experiment=$(jq -n --arg project "$project" --argjson spec "$spec" \
    '{projectId: $project, name: "Smoke test", spec: $spec}' |
    curl -fsS -X POST "$api/experiments" -H 'Content-Type: application/json' -d @- | jq -r '.id')
curl -fsS -X POST "$api/experiments/$experiment/submit" >/dev/null

step "waiting for the simulation (at most ${timeout}s)"
state=queued
waited=0
while [ "$waited" -lt "$timeout" ]; do
    state=$(curl -fsS "$api/experiments/$experiment/status" | jq -r '.state')
    case "$state" in
        succeeded | partial | failed | cancelled) break ;;
    esac
    sleep 2
    waited=$((waited + 2))
done
[ "$state" = succeeded ] || fail "the experiment ended $state after ${waited}s"
echo "succeeded after ${waited}s"

step "reading the results back"
curl -fsS "$api/experiments/$experiment/results" | jq -e 'type == "object"' >/dev/null ||
    fail "the results could not be read"
size=$(curl -fsS "$api/experiments/$experiment/archive" | wc -c)
[ "$size" -gt 0 ] || fail "the archive is empty"
echo "archive: $size bytes"

step "cleaning up"
curl -fsS -X DELETE "$api/projects/$project" >/dev/null

echo "OK"
