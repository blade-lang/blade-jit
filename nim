#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

PROGRAM_ARGS=()
for opt in "$@"
do
  PROGRAM_ARGS+=("$opt")
done

JAVA_ARGS=("--enable-native-access=ALL-UNNAMED" "--sun-misc-unsafe-memory-access=allow")
if [ ! -d "$JAVA_HOME/lib/graalvm" ]; then
    echo "Warning: Could not find GraalVM on $JAVA_HOME. Running on JDK without JIT support."
    echo
fi

"$JAVA_HOME/bin/java" "${JAVA_ARGS[@]}" -cp "${DIR}/build/libs/nimbus-1.0.0.jar" org.nimbus.Main "${PROGRAM_ARGS[@]}"
