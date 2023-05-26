---
name: Bug report
about: Create a report to help us improve
title: 'Bug: '
labels: bug, needs triage
assignees: ''

---

name: Bug Report
description: Report a bug in Timefold Solver
labels: kind/bug
body:
  - type: textarea
    id: description
    validations:
      required: true
    attributes:
      label: Describe the bug
      description: >-
        Describe the issue you are experiencing here. 
        Tell us what you were trying to do and what happened.

        Provide a clear and concise description of what the problem is.
  - type: textarea
    id: expected_behavior
    attributes:
      label: Expected behavior
      description: >-
        Describe the expected behavior clearly and concisely.
  - type: textarea
    id: actual_behavior
    attributes:
      label: Actual behavior
      description: >-
        Describe the actual behavior clearly and concisely.
  - type: textarea
    id: how_to_reproduce
    attributes:
      label: How to Reproduce?
      description: >-
        Link to a small reproducer (preferably a Maven project if the issue is not Gradle-specific) or attach an archive containing the reproducer to the issue.
      placeholder: |
        Reproducer:

        Steps to reproduce the behavior:
        1. 
        2. 
        3.
  - type: markdown
    id: environment
    attributes:
      value: |
        ## Environment
  - type: input
    id: uname
    attributes:
      label: Output of `uname -a` or `ver`
  - type: input
    id: java_version
    attributes:
      label:  Output of `java -version`
  - type: input
    id: timefold_solver_version
    attributes:
      label:  Timefold Solver version of Git ref
  - type: textarea
    id: additional_info
    attributes:
      label: Additional information
      description: >
        If you have any additional information for us, use the field below.
        Please note, you can attach screenshots or screen recordings here, by
        dragging and dropping files in the field below.
