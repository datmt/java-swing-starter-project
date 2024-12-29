@echo off
setlocal

rem Ensure we're in the project root
cd /d "%~dp0\.."

rem Build the project
call mvn clean package

rem Create the app package for Windows
jpackage ^
  --input target ^
  --name "BC18 Spreadsheet Tools" ^
  --main-jar desktop-tools-app-1.0-jar-with-dependencies.jar ^
  --main-class com.toolbox.MainApp ^
  --type msi ^
  --icon src/main/resources/icons/app.ico ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --app-version "1.0" ^
  --vendor "Your Company" ^
  --copyright "Copyright © 2024" ^
  --win-per-user-install ^
  --win-menu-group "BC18 Spreadsheet Tools" ^
  --java-options "-Xmx2048m" ^
  --java-options "--enable-preview"

endlocal
