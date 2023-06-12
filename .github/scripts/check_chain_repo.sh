#!/bin/bash

# Expects the following environment variables to be set:
#   $CHAIN_USER              (Example: "triceo")
#   $CHAIN_BRANCH            (Example: "my_feature_branch")
#   $CHAIN_REPO              (Example: "timefold-quickstarts")
#   $CHAIN_DEFAULT_BRANCH    (Example: "development")
#
# When it terminates, the following Github environment variables will have been set:
#   $TARGET_CHAIN_USER       (Example: "triceo")
#   $TARGET_CHAIN_REPO       (Example: "timefold-quickstarts")
#   $TARGET_CHAIN_BRANCH     (Example: "my_feature_branch")

# Check if the user has cloned the chained repo.
FULL_CHAIN_REPO="https://github.com/$CHAIN_USER/$CHAIN_REPO.git"
echo "Checking if a branch '$CHAIN_BRANCH' exists in '$FULL_CHAIN_REPO'..."
git ls-remote --exit-code $FULL_CHAIN_REPO --heads $CHAIN_BRANCH
if [ $? -ne 0 ]; then
    echo "Branch '$CHAIN_BRANCH' in '$FULL_CHAIN_REPO' does not exist."
    echo "TARGET_CHAIN_USER=TimefoldAI" >> "$GITHUB_ENV"
    echo "TARGET_CHAIN_BRANCH=$CHAIN_DEFAULT_BRANCH" >> "$GITHUB_ENV"
else
    echo "Branch '$CHAIN_BRANCH' in '$FULL_CHAIN_REPO' exists."
    echo "TARGET_CHAIN_USER=$CHAIN_USER" >> "$GITHUB_ENV"
    echo "TARGET_CHAIN_BRANCH=$CHAIN_BRANCH" >> "$GITHUB_ENV"
fi
echo "TARGET_CHAIN_REPO=$CHAIN_REPO" >> "$GITHUB_ENV"