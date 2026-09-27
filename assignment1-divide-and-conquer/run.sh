#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
command -v javac >/dev/null 2>&1 || { echo "JDK 17+ with javac is required"; exit 1; }
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/*.java tests/*.java
TASK=${1:-all}
if [ "$TASK" = "test" ]; then
	java -Djava.awt.headless=true -cp out AllTests
	exit 0
fi
if [ "$TASK" = "all" ]; then
	java -Djava.awt.headless=true -cp out AllTests
fi
java -Djava.awt.headless=true -cp out Main "$TASK"
