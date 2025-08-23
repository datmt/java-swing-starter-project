#!/bin/bash

# Ensure we're in the project root
cd "$(dirname "$0")/.."

# Build the project
mvn clean package

# Create the app package for macOS ARM
jpackage \
  --input target \
  --name "BC18 Spreadsheet Tools" \
  --main-jar swing-starter-jar-with-dependencies.jar \
  --main-class com.datmt.spreadsheet.MainApp \
  --type dmg \
  --icon src/main/resources/icons/app.icns \
  --app-version "1.0" \
  --vendor "NAM SON TECHNOLOGY AND SOLUTIONS CO., LTD" \
  --copyright "Copyright 2024" \
  --mac-package-identifier com.datmt.spreadsheet \
  --mac-package-name "BC18-ST" \
  --java-options "-Xmx2048m" \
  --java-options "--enable-preview"
