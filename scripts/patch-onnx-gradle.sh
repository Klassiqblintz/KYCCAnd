#!/bin/bash
# Patch onnxruntime-react-native build.gradle for Gradle 9.x compatibility
# The original uses VersionNumber.parse() which was removed in Gradle 9.x
# We replace it with the already-extracted REACT_NATIVE_MINOR_VERSION

BUILD_GRADLE="node_modules/onnxruntime-react-native/android/build.gradle"

if [ -f "$BUILD_GRADLE" ]; then
  if grep -q 'VersionNumber.parse(REACT_NATIVE_VERSION)' "$BUILD_GRADLE"; then
    sed -i 's/if (VersionNumber.parse(REACT_NATIVE_VERSION) < VersionNumber.parse("0.71"))/if (REACT_NATIVE_MINOR_VERSION < 71)/' "$BUILD_GRADLE"
    echo "Patched $BUILD_GRADLE for Gradle 9.x compatibility"
  fi
fi
