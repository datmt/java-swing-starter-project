# Packaging Instructions

This directory contains scripts to create native installers for different operating systems using jpackage.

## Prerequisites

1. JDK 17 or later (with jpackage)
2. Maven
3. Platform-specific requirements:
   - **Windows**: WiX Toolset (for MSI creation)
   - **macOS**: Xcode Command Line Tools
   - **Linux**: RPM or DEB build tools depending on target format

## Required Icons

Place the following icons in `src/main/resources/icons/`:
- `app.icns` - macOS icon (required for macOS builds)
- `app.ico` - Windows icon (required for Windows builds)
- `app.png` - Linux icon (required for Linux builds)

## Building Packages

### macOS ARM (Apple Silicon)
```bash
chmod +x package-mac-arm.sh
./package-mac-arm.sh
```

### macOS x64 (Intel)
```bash
chmod +x package-mac-x64.sh
./package-mac-x64.sh
```

### Windows
```batch
package-windows.bat
```

### Linux
```bash
chmod +x package-linux.sh
./package-linux.sh
```

## Output Locations

The installers will be created in the project root directory:
- macOS: `BC18 Spreadsheet Tools-1.0.dmg`
- Windows: `BC18 Spreadsheet Tools-1.0.msi`
- Linux: 
  - `desktop-tools_1.0-1_amd64.deb`
  - `desktop-tools-1.0-1.x86_64.rpm`

## Notes

1. For macOS signing, make sure you have a valid Developer ID Application certificate
2. For Windows, the WiX Toolset must be in the system PATH
3. The Linux script creates both DEB and RPM packages
