#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p docs/api
# Enable the class-only standard @author tag on methods per course requirements.
javadoc -quiet -Xdoclint:all -tag 'author:a:Author:' \
  -classpath lib/acm.jar -d docs/api src/*.java
echo "Generated docs/api/index.html"
