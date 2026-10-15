#!/bin/bash
set -euo pipefail

# Run unit tests
./gradlew test

# Build the APK
./gradlew assemble$BUILD_TYPE

# Rename the APK
APK_DIR=app/build/outputs/apk/$BUILD_TYPE
APK_NAME=$(ls $APK_DIR/*.apk)
NEW_NAME="$APP_NAME-$VERSION-$BUILD_TYPE.apk"
echo "Renaming $APK_NAME to $NEW_NAME"
mv "$APK_NAME" "$APK_DIR/$NEW_NAME"
