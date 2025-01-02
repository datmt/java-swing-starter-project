#!/bin/bash

# Check if a version argument is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

# Ensure we're in the correct directory
echo "Updating version to $1 in pom.xml"

# Create a temporary file to store updated content
temp_file=$(mktemp)

# Replace only the first <version> tag
awk -v new_version="$1" '
    BEGIN { replaced = 0 }
    /<version>/ && replaced == 0 {
        sub(/<version>[0-9A-Za-z._-]+<\/version>/, "<version>" new_version "</version>")
        replaced = 1
    }
    { print }
' pom.xml > "$temp_file"

# Move the temporary file back to pom.xml
mv "$temp_file" pom.xml

# Commit the changes
git add pom.xml
git commit -m "Update version to $1"

# Tag the commit
git tag -a "$1" -m "$1"

# Push changes and the tag
git push
git push --tags

echo "Version updated to $1 and changes pushed to the repository."
