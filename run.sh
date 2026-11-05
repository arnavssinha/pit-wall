#!/usr/bin/env bash
# Runs the prebuilt PitWall.jar. Pass --selftest to run a short timed check and exit.
set -euo pipefail
cd "$(dirname "$0")"
exec java -jar PitWall.jar "$@"
