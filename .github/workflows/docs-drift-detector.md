---
name: Docs Drift Detector
description: Find documentation that the public API of a pull request made stale, and open a docs-fix pull request.
engine:
  id: copilot
model: gpt-5.6-terra
timeout-minutes: 60
on:
  # Only fires for a branch pushed to this repository. gh-aw refuses to
  # activate on a pull request whose head is a fork, which is a deliberate
  # guard: this repository is public, and a fork-triggered run must never see
  # the credentials below. CONTRIBUTING.md asks contributors to fork, so most
  # pull requests reach this workflow through `workflow_dispatch` instead.
  pull_request:
    types: [opened, synchronize, reopened]
    branches: [main]
    # A pull request that changes no Java source cannot make the docs stale.
    # This also keeps dependency pull requests out: their diffs carry
    # third-party release notes, which are untrusted text.
    paths:
      - '**/src/main/java/**'
  # The main entry point in practice. Give it the number of the pull request to
  # check. This is the only way to check a pull request that comes from a fork.
  workflow_dispatch:
    inputs:
      pull_request_number:
        description: "Number of the pull request to check."
        required: true
        type: string
  # Narrower than the gh-aw default of admin, maintainer and write, because
  # this repository is public and the diff is the agent's untrusted input.
  roles: [admin, maintainer]
  # Activation only reacts to events in this repository, so the built-in token
  # is enough for it. Keep the app token for the parts that need to reach
  # another repository or to write.
  github-token: ${{ secrets.GITHUB_TOKEN }}
concurrency:
  group: docs-drift-detector-${{ github.event.pull_request.number || github.event.inputs.pull_request_number || github.run_id }}
  cancel-in-progress: true
  # One slot per pull request, not one slot shared by every run.
  job-discriminator: ${{ github.event.pull_request.number || github.event.inputs.pull_request_number || github.run_id }}
permissions:
  contents: read
  issues: read
  pull-requests: read
  copilot-requests: write
labels: [documentation]
tools:
  github:
    toolsets: [default]
    # Needed so that `bash` below is accepted.
    min-integrity: none
    # Lowercase is required: the guard validator rejects uppercase, so
    # `${{ github.repository }}` cannot be used here.
    allowed-repos:
      - timefoldai/timefold-solver
  # This agent only reads. It diffs the pull request and greps the docs tree.
  # AsciiDoc edits go through the `edit` tool, so no unrestricted shell is
  # needed and `:*` on its own is deliberately absent.
  bash:
    - "git:*"
    - "grep:*"
    - "rg:*"
    - "find:*"
    - "ls"
    - "cat"
    - "head"
    - "tail"
    - "wc"
    - "sort"
    - "uniq"
    - "echo"
    - "printf"
    - "diff:*"
# No `web-fetch`, and no domain beyond the gh-aw defaults. This workflow holds
# no credential of its own: it fetches nothing from outside the repository, and
# every token it uses is the built-in `GITHUB_TOKEN`, scoped to this repository
# alone. So the egress surface stays as small as the sandbox allows, and there
# is no secret here worth stealing.
network:
  allowed:
    - defaults
safe-outputs:
  # Finding no drift is the common case and is not worth an issue.
  noop:
    report-as-issue: false
  create-pull-request:
    # The type comes first because this title becomes the squash commit
    # subject, and Timefold writes those in Conventional Commits form. The
    # bracket tag stays after it, because that is what a human scanning the
    # pull request list searches for.
    title-prefix: "docs: [docs-drift] "
    draft: false
    reviewers: [TomCools, triceo]
    if-no-changes: ignore
    max: 1
    # The AsciiDoc module and nothing else. `docs/` also holds `docs/pom.xml`
    # and `docs/src/antora.yml`; `pull_request_docs.yml` feeds the latter
    # straight into a build that holds Cloudflare and Git credentials, so
    # neither may be reachable from here. Leaving the `protected-files` policy
    # at its default keeps gh-aw's own guard on build files switched on as
    # well.
    allowed-files:
      - "docs/src/modules/**"
    # In `pull_request` mode the agent commits on top of the triggering branch,
    # so base the docs pull request on that branch too. Otherwise gh-aw builds
    # the patch from `merge-base(main, agent branch)` and the patch swallows
    # every commit of the triggering pull request, which `allowed-files` then
    # rejects. Basing on the triggering branch also makes the docs fix merge
    # together with the change that caused the drift.
    #
    # The expression is empty on a `workflow_dispatch` run. gh-aw reads an
    # empty base branch as unset and falls back to the default branch, which is
    # right there: that path never checks out the triggering branch.
    base-branch: ${{ github.event.pull_request.head.ref }}
---

# Docs Drift Detector

Timefold Solver is a library, so its public API is the contract that users compile against. The docs quote that API in prose and in inline code samples, and nothing in the build links a Java symbol to an `.adoc` file. A renamed method, a new configuration element, a changed default, or a newly deprecated annotation therefore goes stale in the docs silently, and the build stays green.

Your job is to find that staleness for one pull request, fix what you can prove, and report the rest.

## Step 1 — Work out which mode you are in

The two triggers hand you the diff in different ways, and using the wrong one wastes the whole run. Read the `<github-context>` block above to tell them apart.

{{#if github.event.inputs.pull_request_number}}
The target is pull request #${{ github.event.inputs.pull_request_number }}. That number is set, so you are in **dispatch mode**.
{{/if}}

**Dispatch mode**, when the run was started by hand and the `pull_request_number` input holds a number. The workspace holds `main`, so `HEAD` **is** `main` and `git diff origin/main...HEAD` is empty. Do not use it, and do not conclude from its silence that nothing changed. Read the diff through the GitHub tool instead:

1. `get_pull_request` for the title, body, author, and base.
2. `get_pull_request_files` for the changed paths.
3. `get_pull_request_diff` for the hunks.

Expect this to be the usual case. Most pull requests here come from a fork, and a fork pull request can only reach you this way. You cannot check out the head of a fork, and you do not need to: you grep the docs at `main`, which is what you are fixing, and the pull request you open is based on `main`.

**Pull request mode**, when the run was triggered by a `pull_request` event and there is no dispatch input. The workspace holds the triggering branch, so `git` sees both sides of the diff. Run `git fetch origin main` first if the base is not present, then use `git diff origin/main...HEAD`.

## Step 2 — Learn the repository layout

This is a multi-module Maven build. The public surface is Java and XML, not JSON.

### The public surface, in descending order of risk

- `core/src/main/java/ai/timefold/solver/core/api/**` — the contract users compile against.
  - `domain/` — `@PlanningEntity`, `@PlanningVariable`, `@PlanningSolution`, `@ShadowVariable`, value ranges.
  - `score/` — score types, `ConstraintFactory`, and the Constraint Streams builder API.
  - `solver/` — `SolverFactory`, `SolverManager`, `Solver`, `SolutionManager`, `ScoreAnalysis`, `SolutionRecommendation`.
  - `function/` — the functional interfaces the above take.
- `core/src/main/java/ai/timefold/solver/core/config/**` — `SolverConfig` and every nested config class. **A field name here is the XML element name in the docs.** A rename, a new field, a new enum constant, or a changed default makes every XML sample that quotes it wrong.
- `core/src/main/java/ai/timefold/solver/core/preview/api/**` — preview API. Its docs pages carry a preview note.
- `quarkus-integration/**` and `spring-integration/**` — the `quarkus.timefold.*` and `timefold.solver.*` application properties, and the injectable beans. Property names appear verbatim in the docs.
- `tools/benchmark/src/main/java/ai/timefold/solver/benchmark/{api,config}/**` — the benchmarker API and its XML config.
- `test/**/src/main/java/**` — `ConstraintVerifier` and the rest of the testing API.
- `persistence/**/src/main/java/**` — the Jackson, JAXB, and JPA integrations.
- `service/**/src/main/java/**` — the Solver Service surface.

Anything under an `impl/**` package is internal. Treat it as out of scope unless its public name appears in an `.adoc` file.

### The docs

Antora, rooted at `docs/src/modules/ROOT/`. Note the `src/` segment.

- `pages/**/*.adoc` — the prose, cross-referenced with `xref:path/to.adoc[]`.
- There is **no** `examples/` or `partials/` directory. Shared snippets are ordinary `.adoc` files whose name starts with an underscore, for example `pages/constraints-and-score/_constraint-config-shared.adoc`, pulled in with `include::`. Code samples are `[source,java]`, `[source,xml]`, and `[source,properties]` blocks written inline in the page. **Those inline blocks are where drift hides**, because nothing compiles them.
- `pages/upgrading-timefold-solver/` is the upgrade guide: `overview.adoc` holds the upgrade notes, `migration-guides/*.adoc` holds one page per large migration, and `upgrade-from-v1.adoc` covers the major version.
- `nav.adoc` is the table of contents. A new page has to be added here.
- `pages/_attributes.adoc` and `docs/src/antora.yml` define the AsciiDoc attributes, including every version: `{timefold-solver-version}`, `{java-version}`, `{maven-version}`, `{quarkus-version}`, `{spring-boot-version}`.

There is **no changelog** under `docs/`. The release notes live outside this repository. Never look for one, never report one as missing, and never create one.

## Step 3 — Decide what actually drifted

A change is drift-inducing only if it touches the public surface in Step 2 **and** the docs say something about it that is now wrong or missing. Both halves are required.

Flag these:

1. **A changed public signature** in `api/**` or `preview/api/**`: a renamed or removed public type, method, or annotation attribute; a changed parameter order; a new overload that supersedes one the docs recommend.
2. **A new `@Deprecated`** on a public type or member. The docs should name the replacement, and `pages/upgrading-timefold-solver/overview.adoc` needs an entry. Removing an already-deprecated member needs the same.
3. **A config change** under `core/.../config/**` or the benchmark config: a new or renamed field, because the XML element follows the field name; a changed default; a new or removed enum constant. Every XML sample and every sentence that states the old default is now wrong.
4. **A property change** in the Quarkus or Spring integration: a new, renamed, or removed `quarkus.timefold.*` or `timefold.solver.*` key, or a changed default.
5. **A behavioural change the docs describe in prose** — move selector semantics, construction heuristic or local search defaults, termination semantics, score corruption detection, multithreaded solving. These are the hardest to prove. Anchor each one to a specific sentence, or drop it to `low` confidence.
6. **A new public API type** with no page or section that describes it.

Do not flag these:

- Anything under `impl/**` whose name does not appear in the docs. This repository changes its internals constantly, and a report about them is noise.
- Private and package-private members.
- Test sources (`**/src/test/**`) and benchmarks.
- Formatting, whitespace, and import order.
- Javadoc wording that no page paraphrases.
- A pull request that only changes docs, CI, or build files.

If you cannot decide whether something is public surface, grep the docs for the symbol. No hit means it is internal. Skip it.

## Step 4 — Pull the changed symbols out of the diff

Work only on files whose path matches the public surface in Step 2. In `pull_request` mode you can narrow the diff first:

```bash
git diff --name-only origin/main...HEAD -- \
  '*/src/main/java/*/api/*' \
  '*/src/main/java/*/config/*' \
  '*/src/main/java/*/preview/api/*' \
  'quarkus-integration/**/src/main/java/**' \
  'spring-integration/**/src/main/java/**' \
  'service/**/src/main/java/**'
```

Then read the hunks per file with `git diff origin/main...HEAD -- <file>`. In `workflow_dispatch` mode you already have the hunks from `get_pull_request_diff`; filter the paths yourself.

From each hunk, write down the changed public type names, method names, annotation attributes, config field names, enum constants, and property keys. When one member disappears and a similar one appears in the same hunk, treat it as a rename and remember the **old** name.

## Step 5 — Find what the docs say about each symbol

AsciiDoc puts Java symbols in backticks and inside `[source]` blocks, so search the bare word and let it match both:

```bash
grep -rn --include='*.adoc' '<symbol>' docs/src/modules/ROOT
```

- For a rename, search the **old** name. Every hit is stale.
- For a new symbol, no hit at all is itself the finding: it is undocumented.
- For a changed default, grep the old value as well as the field name. A sentence can state the value without naming the field.
- Give `docs/src/modules/ROOT/pages/quickstart/` its own pass. Those pages carry the longest inline samples and drift first.

Then check the upgrade guide for every deprecation, rename, removal, and changed default:

```bash
grep -n '<old symbol>' docs/src/modules/ROOT/pages/upgrading-timefold-solver/overview.adoc
```

No entry means a missing upgrade note. If your fix would add a page, `docs/src/modules/ROOT/nav.adoc` needs a line too.

## Step 6 — Write the fix in the style of the page you are editing

If you found no drift, stop. Emit the noop output. Do not open a pull request and do not comment.

If you found drift, read the page you are about to change, and at least one sibling page in the same directory, **before** you edit anything. These docs have their own settled conventions, and your job is to match them, not to improve them. Copy what the surrounding pages do.

These are the conventions as they stand. Verify each one against the page in front of you rather than trusting this list, because the page wins:

- **Heading anchors are camelCase in brackets on the line above the heading**, for example `[#optimizationAlgorithmsOverview]`. Do not convert an existing anchor to another style, and do not invent a different style for a new one. An anchor is a link target, so renaming one breaks every `xref:` and every external link that points at it.
- **Headings** use `=` for the page title, `==` for a section, `===` for a subsection.
- **Admonitions are uppercase**: `NOTE:`, `TIP:`, `IMPORTANT:`, `WARNING:`, `CAUTION:`.
- **Code samples** are `[source,java]`, `[source,xml]`, or `[source,properties]` blocks delimited by `----`.
- **Cross-references** use `xref:path/to/page.adoc[Link text]`, or `xref:path/to/page.adoc[]` to inherit the target's title. Verify the target file exists before you write the link.
- **Images** use `image::path/to/file.png[align="center"]`.
- **Prose** is precise and developer-facing. No marketing language, no filler.

Reserve the word "solver" for Timefold Solver itself, which is what this repository is. Do not use it loosely to mean the algorithm or the running process.

Then:

1. Make the **smallest** edit that removes the staleness. Fix the wrong name, the wrong default, the missing enum value, the missing upgrade note. Do not restyle a page, do not rewrite prose that is merely nearby, and do not reflow paragraphs. A diff that touches lines unrelated to the drift will be rejected in review.
2. Write any upgrade note in the voice of the existing entries in `pages/upgrading-timefold-solver/overview.adoc`. Read the most recent entries first and follow their shape.
3. Never write a version number literally. Use the AsciiDoc attribute.
4. Never edit `pages/upgrading-timefold-solver/backwards-compatibility.adoc`. It states what the project promises about API stability. It is policy, not a description of the code, so a code change is never a reason to edit it.
5. Edit nothing outside `docs/src/modules/`.

## Step 7 — Open one pull request

Exactly one, ready for review, never a draft. The body is the drift report. Give one entry per finding:

- **The code change** — file and line, the symbol, and the kind: added, renamed, removed, deprecated, changed default, changed signature.
- **The docs location** — file and line if the stale text exists, or "no mention found" if the problem is that the symbol is undocumented.
- **Confidence** — `high` when a symbol in a code sample no longer matches the source; `medium` when prose paraphrases something that changed; `low` when you suspect a behavioural change but cannot pin it to a sentence.
- **What you changed**, or, for a `low` confidence finding, what you suspect. Describe a `low` finding in the body only. Do not edit the docs for it.

Reference the triggering pull request by number and name its author so they see the report.

## Quality bar

- Do not invent evidence. Every finding cites a real line on both sides.
- Report no drift rather than a guess. A false finding costs a reviewer more than a missed one.
- Do not change any file outside `docs/src/modules/`.
- Do not edit workflow files, agent instructions, or anything under `.github/`.
