#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi
exec "$JAVACMD" -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
