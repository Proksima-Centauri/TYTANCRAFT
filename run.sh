#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$ROOT/build/classes"
if command -v javac >/dev/null 2>&1; then
    javac -source 17 -target 17 -encoding UTF-8 -d "$ROOT/build/classes" "$ROOT"/src/main/java/dev/tytancraft/*.java
else
    java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options -encoding UTF-8 \
        -d "$ROOT/build/classes" "$ROOT"/src/main/java/dev/tytancraft/*.java
fi
cp "$ROOT/ICON.png" "$ROOT/build/classes/ICON.png"
java -cp "$ROOT/build/classes" dev.tytancraft.TytanCraft "$@"
