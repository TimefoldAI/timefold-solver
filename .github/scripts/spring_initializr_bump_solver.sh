#!/bin/bash

# Expects the following environment variables to be set:
#   $SPRING_INITIALIZR_YAML_FILE_PATH   (Example: "application.yml")
#   $TIMEFOLD_SOLVER_VERSION            (Example: "1.6.0")

# Temporary file
temp_file="temp.yml"

# Flag to indicate if the 'timefold-solver' keyword is found
found_keyword=false

# Read the YAML file line by line
while IFS= read -r line
do
    # Check if the 'timefold-solver' keyword is found
    if [[ "$line" =~ "timefold-solver" ]]; then
        found_keyword=true
    fi

    # Replace the first 'version' line after finding the keyword
    if [[ $found_keyword == true && "$line" =~ "version:" ]]; then
        echo "${line/version:*/version: $TIMEFOLD_SOLVER_VERSION}" >> "$temp_file"
        found_keyword=false
    else
        echo "$line" >> "$temp_file"
    fi
done < "$SPRING_INITIALIZR_YAML_FILE_PATH"

# Compare the original file with the modified temporary file
if diff -q "$SPRING_INITIALIZR_YAML_FILE_PATH" "$temp_file" > /dev/null; then
    rm "$temp_file"
    echo "No changes were made to the file."
    exit 1
else
    # Replace the original file with the modified temporary file
    mv "$temp_file" "$SPRING_INITIALIZR_YAML_FILE_PATH"
fi