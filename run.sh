#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$ROOT/build/classes"
if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -d "$ROOT/build/classes" "$ROOT"/src/main/java/dev/tytancraft/*.java
else
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d "$ROOT/build/classes" "$ROOT"/src/main/java/dev/tytancraft/*.java
fi
java -cp "$ROOT/build/classes" dev.tytancraft.TytanCraft "$@"
