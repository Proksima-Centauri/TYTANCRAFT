#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -d "$ROOT/src/main/java/dev/tytancraft" ]; then
    CLASSES="$ROOT/build/classes"
    mkdir -p "$CLASSES"
    if command -v javac >/dev/null 2>&1; then
        javac -source 17 -target 17 -encoding UTF-8 -d "$CLASSES" "$ROOT"/src/main/java/dev/tytancraft/*.java
    else
        java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options \
            -encoding UTF-8 -d "$CLASSES" "$ROOT"/src/main/java/dev/tytancraft/*.java
    fi
else
    CLASSES="$ROOT/TytanCraft.jar"
fi
java -cp "$CLASSES" dev.tytancraft.GameServer "$@"
