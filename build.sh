#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 11 -Xlint:all,-serial -Werror -encoding UTF-8 \
  -cp lib/acm.jar -d build/classes src/*.java
printf 'Main-Class: PitWallDemo\nClass-Path: ../lib/acm.jar\n' > build/manifest.txt
jar cfm build/PitWallDemo.jar build/manifest.txt -C build/classes .
echo "Built build/PitWallDemo.jar"
