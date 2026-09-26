#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/out"
mkdir -p "$OUT"
find "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print > "$OUT/sources.txt"
javac -d "$OUT" @"$OUT/sources.txt"
java -cp "$OUT" learning.fintech.report.ReportPolicyTest

