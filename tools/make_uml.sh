#!/usr/bin/env bash
# Regenerates docs/uml/classes.puml and docs/uml/classes.png from the source code.
set -euo pipefail
cd "$(dirname "$0")/.."
unset JAVA_TOOL_OPTIONS 2>/dev/null || true
java -Djava.awt.headless=true tools/UmlDiagram.java
