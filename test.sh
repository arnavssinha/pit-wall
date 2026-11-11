#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
bash build.sh
mkdir -p build/test
javac --release 11 -Xlint:all,-serial -Werror -encoding UTF-8 \
  -cp "build/classes:lib/acm.jar" -d build/test test/*.java
java -Djava.awt.headless=true \
  -cp "build/classes:build/test:lib/acm.jar" GeometryTest
