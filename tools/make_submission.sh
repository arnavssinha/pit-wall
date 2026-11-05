#!/usr/bin/env bash
# Builds submission/: every .java file from src/ and test/ copied flat, plus the UML PNG.
# Canvas only accepts java, pdf, jpg and png, so nothing else goes in.
# tools/*.java are developer helpers (UML drawing, track drawing) and are left out.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf submission
mkdir -p submission
cp src/*.java submission/
cp test/*.java submission/
if [ -f docs/uml/classes.png ]; then
    cp docs/uml/classes.png submission/PitWall_UML.png
else
    echo "warning: docs/uml/classes.png not found, run tools/make_uml.sh first" >&2
fi
for pdf in docs/*.pdf; do
    [ -e "$pdf" ] && cp "$pdf" submission/
done
echo "submission/ now holds:"
ls -1 submission
echo "$(ls -1 submission | wc -l | tr -d ' ') files"
