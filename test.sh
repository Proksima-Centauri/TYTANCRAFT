#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$ROOT/build/test-classes"
if command -v javac >/dev/null 2>&1; then
    javac -source 17 -target 17 -Xlint:all,-options -encoding UTF-8 -d "$ROOT/build/test-classes" \
        "$ROOT"/src/main/java/dev/tytancraft/*.java "$ROOT"/src/test/java/dev/tytancraft/*.java
else
    java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:all,-options -encoding UTF-8 -d "$ROOT/build/test-classes" \
        "$ROOT"/src/main/java/dev/tytancraft/*.java "$ROOT"/src/test/java/dev/tytancraft/*.java
fi
java -Djava.awt.headless=true -Dtytancraft.disableBlockRender=true \
    -cp "$ROOT/build/test-classes" dev.tytancraft.CoreBehaviorTest
