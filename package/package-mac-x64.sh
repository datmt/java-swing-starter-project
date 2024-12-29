#!/bin/bash

# Ensure we're in the project root
cd "$(dirname "$0")/.."

# Build the project
mvn clean package

# Create the app package for macOS x64
jpackage \
  --input target \
  --name "BC18 Spreadsheet Tools" \
  --main-jar desktop-tools-app-1.0-jar-with-dependencies.jar \
  --main-class com.toolbox.MainApp \
  --type dmg \
  --icon src/main/resources/icons/app.icns \
  --app-version "1.0" \
  --vendor "Your Company" \
  --copyright "Copyright 2024" \
  --mac-package-identifier com.toolbox.desktoptools \
  --mac-package-name "BC18 Spreadsheet Tools" \
  --java-options "-Xmx2048m" \
  --java-options "--enable-preview"
