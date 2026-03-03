# Contributing to Timefold Solver

First off, thanks for showing willingness to contribute to this repository by looking at this file.
We are happy to have you here!

All types of contributions are encouraged and valued. In this file, we list different ways you can help the project and details about how this project handles them.

## Q&A 

### "I have a question"

Before you ask a question, it is best to search through our extensive [documentation](https://docs.timefold.ai/timefold-solver/latest/introduction) and for existing [issues](/issues).  
It is also advisable to check the internet and/or ask your favorite LLM.

If you can't find a satisfying answer, post a message on our [GitHub discussions](https://github.com/TimefoldAI/timefold-solver/discussions) or on
[our Discord server](https://discord.gg/976RcEVVHW).

### "I have (found) an issue"

First check if we already have a ticket for your problem by looking at our existing [Issues](/issues).

- In case you have found a suitable issue and still need clarification, you can write your question in this issue.
- If the issue does not exist yet, please create a new one using the appropriate template (Bug report, feature request, ...).

### "I have an idea to improve Timefold"

We'd love to hear your ideas!
Talk to us about them [here on GitHub](https://github.com/TimefoldAI/timefold-solver/discussions) or on
[our Discord server](https://discord.gg/976RcEVVHW)

### "I want to help out!"

Excellent! We have issues labeled specifically for first time contributors. 
You can find them either on: 

- [Our quickstart repository](https://github.com/TimefoldAI/timefold-quickstarts/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22good%20first%20issue%22)
- [The Timefold Solver repository](https://github.com/TimefoldAI/timefold-solver/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22good%20first%20issue%22)

If you are starting out, we recommend working on the quickstarts first, as those usually require no knowledge of the internal architecture of the solver.

> [!NOTE]
> The quickstart repository has a separate [CONTRIBUTING](https://github.com/TimefoldAI/timefold-quickstarts?tab=contributing-ov-file#contributing-to-timefold-quickstarts) file which is slightly different from the one you are reading now.

## Development

> [!IMPORTANT]
> **All contributions must comply with the [Timefold Solver Constitution](.specify/memory/constitution.md).**
> The constitution defines our core principles, technology stack, code quality standards, and development practices. Key areas include:
> - **Core Principles**: Fail Fast, Understandable Error Messages, Consistent Terminology, Real World Usefulness, Automated Testing, Good Code Hygiene
> - **Technology Stack**: JDK 21 compatibility, nullability policy, dependency constraints, test infrastructure, security requirements
> - **Code Quality**: SonarCloud quality gates (Reliability ≥ B, Maintainability ≥ B), code coverage requirements, automated formatting
> - **Package Structure**: Public API (`api` packages) and Configuration (`config` packages) are 100% backwards compatible; Implementation can change freely
> 
> Please review the constitution before contributing to understand the project's standards and requirements.

---

### From idea to Pull Request

Once you have selected an issue you want to work on:

1. [Fork Timefold Solver](https://github.com/TimefoldAI/timefold-solver/fork).
2. Create a feature branch: `git checkout -b feature`
3. Commit your changes with a comment: `git commit -m "feat: add shiny new feature"`  
   (See [Commit Messages](#commit-messages) for details.)
4. Push to the branch to GitHub: `git push origin feature`
5. [Create a new Pull Request](https://github.com/TimefoldAI/timefold-solver/compare).

The CI checks against your PR to ensure that it doesn't introduce errors.
If the CI identifies a potential problem, our friendly PR maintainers will help you resolve it.

---

### Build the Timefold Solver Project

Use one of the following ways to build this project:

- 🚀 **build-fast**: `./mvnw clean install -Dquickly` - Skips checks and code analysis (~1 min)
- 🔨 **build-normally**: `./mvnw clean install` - Runs tests, checks code style, skips documentation (~17 min)
- 📄 **build-doc**: `./mvnw clean install` in the `docs` directory - Creates asciidoctor documentation `docs/target/html_single/index.html` (~2 min)
- 🦾 **build-all**: `./mvnw clean install -Dfull` - Runs all checks, creates documentation and distribution files (~20 min)

---

### Code Style

Your code is automatically formatted according to the project conventions during every Maven build. 
CI checks enforce those conventions too, so be sure to build your project with Maven before creating your PR:

```bash
./mvnw clean install
```

For information about how to set up code style checks in your IDE, see [IDE Setup Instructions](build/ide-config/ide-configuration.adoc).

**Key code style conventions** (see [Constitution](.specify/memory/constitution.md) for complete details):
- Automatic formatting via Maven build
- Use newlines sparingly to separate logical blocks
- When multiple class fields are touched, use/modify them in declaration order
- Follow standard Java conventions

---

### Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for PR titles and commit messages.

This convention is enforced by CI checks on pull requests.

---

### Quality Gates

All PRs must pass the following automated checks:

#### SonarCloud Quality Gates
- **Reliability**: Grade must be B or better (strive for A)
- **Maintainability**: Grade must be B or better (strive for A)
- **Code Coverage**: Must not fall below the configured threshold

PRs that worsen grades below B will fail CI and cannot be merged.

#### Security Scanning
- **Aikido**: Performs automated security checks
- **Dependabot**: Provides weekly dependency upgrade PRs
- Code must not contain hardcoded credentials, secrets, or sensitive data

#### Testing Requirements
- All code must have tests before being merged
- Use **JUnit** for test execution
- Use **AssertJ** for all assertions (JUnit assertions are forbidden)
- Mockito allowed for mocking; prefer real objects when practical