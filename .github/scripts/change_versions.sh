#!/bin/bash

# Expects the following environment variables to be set:
#   $NEW_VERSION                 (Example: "1.2.0")
# This will fail the Maven build if the version is not available.
# Thankfully, this is not the case (yet) in this project.
# If/when it happens, this needs to be replaced by a manually provided version,
# as scanning the text of the POM would be unreliable.
echo "     New version: $NEW_VERSION"
./mvnw versions:set -Dfull -Dproperty=revision -DnewVersion="$NEW_VERSION" -Drevision="$NEW_VERSION" -DgenerateBackupPoms=false
# replaces the parent revision which is not updated by the previous command
sed -i "0,/<\/revision>/s/<revision>.*<\/revision>/<revision>$NEW_VERSION<\/revision>/" pom.xml
sed -i "0,/<\/revision>/s/<revision>.*<\/revision>/<revision>$NEW_VERSION<\/revision>/" service/facade/service-parent/pom.xml
git add -u
