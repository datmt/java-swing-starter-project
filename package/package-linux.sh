#!/bin/bash

# Ensure we're in the project root
cd "$(dirname "$0")/.."

# Build the project
mvn clean package

# Create the app package for Linux
jpackage \
  --input target \
  --name "desktop-tools" \
  --main-jar bc-spreadsheet-jar-with-dependencies.jar \
  --main-class com.binarycarpenter.spreadsheet.MainApp \
  --type deb \
  --icon src/main/resources/icons/app.png \
  --app-version "1.0" \
  --vendor "Binary Carpenter" \
  --copyright "Copyright © 2024" \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --linux-app-category "Utility" \
  --linux-package-name "desktop-tools" \
  --java-options "-Xmx2048m" \
  --java-options "--enable-preview"

# Also create RPM package
jpackage \
  --input target \
  --name "desktop-tools" \
  --main-jar bc-spreadsheet-jar-with-dependencies.jar \
  --main-class com.binarycarpenter.spreadsheet.MainApp \
  --type rpm \
  --icon src/main/resources/icons/app.png \
  --app-version "1.0" \
  --vendor "NAM SON TECHNOLOGY AND SOLUTIONS CO., LTD" \
  --copyright "Copyright © 2024" \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --linux-app-category "Utility" \
  --linux-package-name "desktop-tools" \
  --java-options "-Xmx2048m" \
  --java-options "--enable-preview"
