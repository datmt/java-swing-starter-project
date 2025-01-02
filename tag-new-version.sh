#!/bin/bash

# Ensure we're in the project root
echo "Updating version to $1"
#cd "$(dirname "$0")/.."
# Update the version in the pom.xml file

# Linux
#sed -i "s/<version>.*<\/version>/<version>$1<\/version>/" pom.xml

# Mac
sed -i '' "s/<version>.*<\/version>/<version>$1<\/version>/" pom.xml

# Commit the changes
git add pom.xml
git commit -m "Update version to $1"

git tag -a "$1" -m "$1"

# Push the changes
git push
git push --tags
