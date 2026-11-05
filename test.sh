#!/usr/bin/env bash
# Compiles the game plus test/SimTests.java and runs the tests with no display (headless).
# Exit code is 0 only if every test passes.
set -euo pipefail
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS 2>/dev/null || true

rm -rf build/test
mkdir -p build/test
javac --release 11 -Xlint:all -Werror -encoding UTF-8 \
      -cp lib/acm.jar -d build/test src/*.java test/*.java
# The project root is on the classpath so data/... resolves as a resource.
java -Djava.awt.headless=true -cp "build/test:lib/acm.jar:." SimTests
