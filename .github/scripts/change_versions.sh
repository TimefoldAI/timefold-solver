#!/bin/bash

# Expects the following environment variables to be set:
#   $NEW_VERSION                 (Example: "1.2.0")

echo "     New version: $NEW_VERSION"
sed -i "0,/<\/revision>/s/<revision>.*<\/revision>/<revision>$NEW_VERSION<\/revision>/" pom.xml
sed -i "0,/<\/revision>/s/<revision>.*<\/revision>/<revision>$NEW_VERSION<\/revision>/" service/facade/service-parent/pom.xml
git add -u
