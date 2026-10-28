#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSES="$ROOT/build/package-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
if command -v javac >/dev/null 2>&1; then
    javac -source 17 -target 17 -Xlint:all,-options -encoding UTF-8 -d "$CLASSES" "$ROOT"/src/main/java/dev/tytancraft/*.java
else
    java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:all,-options -encoding UTF-8 \
        -d "$CLASSES" "$ROOT"/src/main/java/dev/tytancraft/*.java
fi
cp "$ROOT/ICON.png" "$CLASSES/ICON.png"

if command -v jar >/dev/null 2>&1; then
    jar --create --file "$ROOT/build/TytanCraft.jar" --main-class dev.tytancraft.TytanCraft \
        -C "$CLASSES" .
else
    java -m jdk.jartool/sun.tools.jar.Main --create --file "$ROOT/build/TytanCraft.jar" \
        --main-class dev.tytancraft.TytanCraft -C "$CLASSES" .
fi
