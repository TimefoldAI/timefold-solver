# GitHub Copilot Code Review Instructions

Read [CONSTITUTION.md](/CONSTITUTION.md) before reviewing. Comment only with >80% confidence. One sentence per comment.

## Review: Yes

**Security**: command injection, path traversal, hardcoded secrets, missing input validation, error handling leaking sensitive info.

**Correctness**: logic errors, race conditions, resource leaks, off-by-one errors, incorrect error propagation.

**Architecture**: pattern violations, inconsistent library use, missing error handling, inconsistent naming, comments that restate the code.

## Review: No

Style/formatting (CI), test failures (CI), suggestions to add comments, refactoring without clear bug, multiple issues per comment.

## Format

1. Problem (1 sentence)
2. Why it matters (1 sentence, only if not obvious)
3. Suggested fix (snippet or action)

If uncertain, stay silent.
