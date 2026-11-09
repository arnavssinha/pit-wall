#!/usr/bin/env bash
# Checks the course rules. Prints PASS or FAIL per check; the exit code is the number of failures.
cd "$(dirname "$0")/.."
unset JAVA_TOOL_OPTIONS 2>/dev/null || true
fails=0

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; echo "$2" | sed 's/^/      /'; fails=$((fails + 1)); }

# $1 = label, then grep arguments. Passes when grep finds nothing.
must_be_empty() {
    local label=$1; shift
    local out; out=$(grep "$@" 2>/dev/null)
    if [ -z "$out" ]; then pass "$label"; else fail "$label" "$out"; fi
}

must_be_empty "no wildcard imports" -rnE '^\s*import .*\*;' src test
must_be_empty "no /*** or **/ comment markers" -rnE '/\*{3,}|\*{2,}/' src test
must_be_empty "no Swing or custom painting (ACM only)" -rn 'javax.swing\|paintComponent\|java.awt.Graphics' src
must_be_empty "simulation classes do not import graphics" -nE 'import (acm|java\.awt)' \
    src/Track.java src/Compound.java src/Car.java src/Fight.java src/Ai.java src/Race.java

# Every class, enum, field, constructor and method (at member indentation) has a /** */ comment above it.
missing=$(awk '
    FNR == 1 { prev = "" }
    /^( {4}| {8})?(public|private|protected|static|final)[ ]/ || /^ {4,8}[A-Z_]+[(,]/ || /^ {4,8}[A-Z_]+$/ {
        if (prev !~ /\*\/$/ && prev !~ /@Override/) print FILENAME ":" FNR ": " $0
    }
    NF > 0 { prev = $0 }
' src/*.java test/*.java)
if [ -z "$missing" ]; then pass "every class, field and method has a Javadoc comment"; else fail "missing Javadoc" "$missing"; fi

mkdir -p build/audit
if javac --release 11 -Xlint:all,-serial -Werror -encoding UTF-8 -cp lib/acm.jar -d build/audit src/*.java test/*.java > build/audit/javac.txt 2>&1; then
    pass "javac reports zero warnings"
else
    fail "javac warnings" "$(head -30 build/audit/javac.txt)"
fi
if javadoc -Xdoclint:all,-missing -private -quiet -encoding UTF-8 -d build/audit/api -cp lib/acm.jar src/*.java > build/audit/javadoc.txt 2>&1 \
   && ! grep -qiE 'warning|error' build/audit/javadoc.txt; then
    pass "javadoc builds with no errors"
else
    fail "javadoc problems" "$(head -30 build/audit/javadoc.txt)"
fi

echo
if [ "$fails" -eq 0 ]; then echo "AUDIT: all checks passed"; else echo "AUDIT: $fails check(s) failed"; fi
exit "$fails"
