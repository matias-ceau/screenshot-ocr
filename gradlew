#!/bin/sh

# Gradle wrapper script that downloads and uses the specified Gradle version

GRADLE_VERSION="8.5"
GRADLE_HOME="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
GRADLE_ZIP_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

# Find or download Gradle
if [ -d "$GRADLE_HOME" ]; then
    GRADLE_DIR=$(find "$GRADLE_HOME" -maxdepth 2 -name "gradle-${GRADLE_VERSION}" -type d 2>/dev/null | head -1)
fi

if [ -z "$GRADLE_DIR" ] || [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_HOME"
    HASH=$(echo "$GRADLE_ZIP_URL" | md5sum | cut -d' ' -f1)
    DOWNLOAD_DIR="$GRADLE_HOME/$HASH"
    mkdir -p "$DOWNLOAD_DIR"
    
    if [ ! -f "$DOWNLOAD_DIR/gradle-${GRADLE_VERSION}-bin.zip" ]; then
        curl -L -o "$DOWNLOAD_DIR/gradle-${GRADLE_VERSION}-bin.zip" "$GRADLE_ZIP_URL"
    fi
    
    if [ ! -d "$DOWNLOAD_DIR/gradle-${GRADLE_VERSION}" ]; then
        echo "Extracting Gradle..."
        unzip -q "$DOWNLOAD_DIR/gradle-${GRADLE_VERSION}-bin.zip" -d "$DOWNLOAD_DIR"
    fi
    
    GRADLE_DIR="$DOWNLOAD_DIR/gradle-${GRADLE_VERSION}"
fi

exec "$GRADLE_DIR/bin/gradle" "$@"
