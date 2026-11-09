#!/usr/bin/env bash
# Builds Pit Wall: compiles every file in src/ and packs PitWall.jar at the repo root.
# The jar's manifest points at lib/acm.jar, so keep the lib folder next to the jar.
set -euo pipefail
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS 2>/dev/null || true

rm -rf build/classes
mkdir -p build/classes

echo "Compiling src/*.java (release 11, all lint warnings are errors)..."
javac --release 11 -Xlint:all,-serial -Werror -encoding UTF-8 \
      -cp lib/acm.jar -d build/classes src/*.java

# Data files travel inside the jar so a double-clicked jar finds them
# no matter which folder macOS starts it in.
cp -R data build/classes/data

printf 'Main-Class: PitWall\nClass-Path: lib/acm.jar\n' > build/manifest.txt
jar cfm PitWall.jar build/manifest.txt -C build/classes .
echo "Built PitWall.jar"
