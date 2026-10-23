#!/usr/bin/env sh

# Resolusi direktori tempat skrip berjalan
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Tentukan lokasi jar wrapper
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Cek ketersediaan java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Jalankan Gradle via wrapper jar
exec "$JAVACMD" "-Dorg.gradle.appname=gradlew" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
