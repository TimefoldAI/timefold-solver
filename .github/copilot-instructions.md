# GitHub Copilot Code Review Instructions

## Review Philosophy
- You must read the project's [constitution](/.specify/memory/constitution.md) and use it to guide your code review
- Only comment when you have HIGH CONFIDENCE (>80%) that an issue exists
- Be concise: one sentence per comment when possible
- Focus on actionable feedback, not observations
- When reviewing text, only comment on clarity issues if the text is genuinely confusing or could lead to errors.

## Priority Areas (Review These)

### Security & Safety
- Command injection risks (shell commands, user input)
- Path traversal vulnerabilities
- Credential exposure or hardcoded secrets
- Missing input validation on external data
- Improper error handling that could leak sensitive info

### Correctness Issues
- Logic errors that could cause exceptions or incorrect behavior
- Race conditions in async code
- Resource leaks (files, connections, memory)
- Off-by-one errors or boundary conditions
- Incorrect error propagation
- Unnecessary comments that just restate what the code already shows (remove them)

### Architecture & Patterns
- Code that violates existing patterns in the codebase
- Inconsistent use of libraries or frameworks
- Missing error handling
- Inconsistent naming of methods, variables, classes

## Skip These (Low Value)

Do not comment on:
- **Style/formatting** - CI handles this
- **Test failures** - CI handles this (full test suite)
- **Suggestions to add comments** - for self-documenting code
- **Refactoring suggestions** - unless there's a clear bug or maintainability issue
- **Multiple issues in one comment** - avoid combining issues; write a separate comment for each and focus each on the single most critical issue

## Response Format

When you identify an issue:
1. **State the problem** (1 sentence)
2. **Why it matters** (1 sentence, only if not obvious)
3. **Suggested fix** (code snippet or specific action)

## When to Stay Silent

If you're uncertain whether something is an issue, don't comment. 
False positives create noise and reduce trust in the review process.