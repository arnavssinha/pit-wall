#!/usr/bin/env bash
# Prints where each data structure required by the assignment (rule R3) is used,
# so the final audit can point at real files and lines.
cd "$(dirname "$0")/.."
show() {
    local label=$1 pattern=$2
    echo "== $label =="
    grep -nE "$pattern" src/*.java | grep -vE '^\S+:[0-9]+:\s*(\*|//|/\*\*)' | sed 's/^/  /' | head -12
    echo
}
show "2D arrays"              '\[\]\[\]'
show "ArrayList"              'new ArrayList'
show "List interface"         '(^|[^A-Za-z])List<'
show "PriorityQueue"          'PriorityQueue'
show "HashMap"                'HashMap'
show "ArrayDeque / Deque"     'Deque'
show "Collections.sort / Comparator" 'Collections\.sort|Comparator'
show "TreeMap"                'TreeMap'
show "Random (seeded)"        'new Random\('
