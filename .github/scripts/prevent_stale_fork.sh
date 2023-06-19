#!/bin/bash

# Expects the following environment variables to be set:
#   $BLESSED_REPO      (Example: "timefold-quickstarts")
#   $BLESSED_BRANCH    (Example: "development")
#
# When it terminates, the clone of the fork will be rebased onto blessed latest.

# Identify ourselves, otherwise rebase will fail.
git config --local user.name "Timefold CI"
git config --local user.email "ci@timefold.ai"

# Rebase the fork onto the latest.
echo "Will merge current branch onto '$BLESSED_BRANCH' from 'https://github.com/TimefoldAI/$BLESSED_REPO.git'"
git remote add upstream https://github.com/TimefoldAI/$BLESSED_REPO.git
git fetch upstream $BLESSED_BRANCH
git merge upstream/$BLESSED_BRANCH

if [ $? -ne 0 ]; then
    echo "Merge failed, trying rebase."
    echo "Please make sure your fork is up to date."
    exit 1
fi