#!/usr/bin/env bash
# Compliance checks from the project brief (Section 10). Prints PASS or FAIL per check.
# Exit code is the number of failed checks, so 0 means everything passed.
cd "$(dirname "$0")/.."
unset JAVA_TOOL_OPTIONS 2>/dev/null || true
fails=0

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; fails=$((fails + 1)); }

# Runs a grep that must find nothing. $1 = label, rest = grep arguments.
must_be_empty() {
    local label=$1; shift
    local out
    out=$(grep "$@" 2>/dev/null)
    if [ -z "$out" ]; then pass "$label"; else fail "$label"; echo "$out" | sed 's/^/      /'; fi
}

echo "== Javadoc asterisk rule and imports =="
must_be_empty "no /*** openers"                -rnE '/\*{3,}' src test
must_be_empty "no **/ closers"                 -rnE '\*{2,}/' src test
must_be_empty "no lines starting with **"      -rnE '^\s*\*{2,}' src test
must_be_empty "no wildcard imports"            -rnE '^\s*import .*\*;' src test

echo "== ACM rule =="
must_be_empty "no swing, paintComponent or awt.Graphics in src" -rn 'javax.swing\|paintComponent\|java.awt.Graphics' src
if grep -q 'class PitWall extends GraphicsProgram' src/PitWall.java; then
    pass "PitWall extends GraphicsProgram"
else
    fail "PitWall extends GraphicsProgram"
fi

echo "== Layer rule (tools/layers.txt) =="
layer_of() { awk -v f="$1" '$2 == f { print $1 }' tools/layers.txt; }
unlisted=""
for path in src/*.java; do
    name=$(basename "$path")
    [ -z "$(layer_of "$name")" ] && unlisted="$unlisted $name"
done
if [ -z "$unlisted" ]; then pass "every src file is listed in layers.txt"; else fail "files missing from layers.txt:$unlisted"; fi

missing=""
headless=""
for name in $(awk '!/^#/ && NF == 2 { print $2 }' tools/layers.txt); do
    [ -f "src/$name" ] || missing="$missing $name"
    case $(layer_of "$name") in sim|ai|data|lab) headless="$headless src/$name";; esac
done
if [ -z "$missing" ]; then pass "every layers.txt entry exists"; else fail "layers.txt lists missing files:$missing"; fi
if [ -n "$headless" ]; then
    # shellcheck disable=SC2086
    must_be_empty "headless layers import no acm, java.awt or javax.swing" -nE 'import (acm|java\.awt|javax\.swing)' $headless
fi

ui_files=""
for name in $(awk '$1 == "ui" { print $2 }' tools/layers.txt); do ui_files="$ui_files src/$name"; done
if [ -n "$ui_files" ]; then
    # shellcheck disable=SC2086
    bad=$(grep -hE '^import ' $ui_files | sort -u | grep -vE '^import (acm\.graphics\.[A-Za-z]+|acm\.program\.[A-Za-z]+|java\.awt\.Color|java\.awt\.Font|java\.awt\.event\.(KeyEvent|MouseEvent)|java\.util\.[A-Za-z]+|java\.io\.IOException);$')
    if [ -z "$bad" ]; then pass "ui imports only ACM, Color, Font, key and mouse events, java.util and IOException"
    else fail "ui imports outside the allowed list"; echo "$bad" | sed 's/^/      /'; fi
fi

echo "== Compiler and Javadoc warnings =="
rm -rf build/audit && mkdir -p build/audit
if javac --release 11 -Xlint:all -Werror -encoding UTF-8 -cp lib/acm.jar -d build/audit src/*.java test/*.java > build/audit/javac.txt 2>&1; then
    pass "javac -Xlint:all reports zero warnings"
else
    fail "javac -Xlint:all"; sed 's/^/      /' build/audit/javac.txt | head -30
fi
if javadoc -Xdoclint:all -private -quiet -encoding UTF-8 -d docs/api -cp lib/acm.jar src/*.java > build/audit/javadoc.txt 2>&1 \
   && ! grep -qiE 'warning|error' build/audit/javadoc.txt; then
    pass "javadoc -Xdoclint:all -private reports zero warnings"
else
    fail "javadoc -Xdoclint:all"; sed 's/^/      /' build/audit/javadoc.txt | head -30
fi

echo
if [ "$fails" -eq 0 ]; then echo "AUDIT: all checks passed"; else echo "AUDIT: $fails check(s) failed"; fi
exit "$fails"
