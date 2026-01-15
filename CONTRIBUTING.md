# Contributing to Timefold Solver

This is an open source project, and you are more than welcome to contribute!

- Found an issue? [Submit an issue](https://github.com/TimefoldAI/timefold-solver/issues).
- Want to fix an issue or contribute an improvement? 
Talk to us about your ideas [here on Github](https://github.com/TimefoldAI/timefold-solver/discussions), 
[on Discord](https://discord.gg/976RcEVVHW)
or just start coding:

1. [Fork it](https://github.com/TimefoldAI/timefold-solver/fork).
2. Create a feature branch: `git checkout -b feature`
3. Commit your changes with a comment: `git commit -m "feat: add shiny new feature"`  
   (See [Commit Messages](#commit-messages) for details.)
4. Push to the branch to GitHub: `git push origin feature`
5. [Create a new Pull Request](https://github.com/TimefoldAI/timefold-solver/compare).

The CI checks against your PR to ensure that it doesn't introduce errors.
If the CI identifies a potential problem, our friendly PR maintainers will help you resolve it.

---

## Development Philosophy and Standards

**All contributions must comply with the [Timefold Solver Constitution](.specify/memory/constitution.md).**

The constitution defines our core principles, technology stack, code quality standards, and development practices. Key areas include:

- **Core Principles**: Fail Fast, Understandable Error Messages, Consistent Terminology, Real World Usefulness, Automated Testing, Good Code Hygiene
- **Technology Stack**: JDK 17 compatibility, nullability policy, dependency constraints, test infrastructure, security requirements
- **Code Quality**: SonarCloud quality gates (Reliability ≥ B, Maintainability ≥ B), code coverage requirements, automated formatting
- **Package Structure**: Public API (`api` packages) and Configuration (`config` packages) are 100% backwards compatible; Implementation can change freely

Please review the constitution before contributing to understand the project's standards and requirements.

---

## Build the Timefold Solver Project

Use one of the following ways to build this project:

- 🚀 **build-fast**: `./mvnw clean install -Dquickly` - Skips checks and code analysis (~1 min)
- 🔨 **build-normally**: `./mvnw clean install` - Runs tests, checks code style, skips documentation (~17 min)
- 📄 **build-doc**: `./mvnw clean install` in the `docs` directory - Creates asciidoctor documentation `docs/target/html_single/index.html` (~2 min)
- 🦾 **build-all**: `./mvnw clean install -Dfull` - Runs all checks, creates documentation and distribution files (~20 min)

---

## Code Style

Your code is automatically formatted according to the project conventions during every Maven build. CI checks enforce those conventions too, so be sure to build your project with Maven before creating your PR:

```bash
./mvnw clean install
```

For information about how to set up code style checks in your IDE, see [IDE Setup Instructions](https://github.com/TimefoldAI/timefold-solver/blob/main/build/ide-config/ide-configuration.adoc).

**Key code style conventions** (see [Constitution](.specify/memory/constitution.md) for complete details):
- Automatic formatting via Maven build
- Use newlines sparingly to separate logical blocks
- When multiple class fields are touched, use/modify them in declaration order
- Follow standard Java conventions

---

## Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for PR titles and commit messages.

This convention is enforced by CI checks on pull requests.

---

## Quality Gates

All PRs must pass the following automated checks:

### SonarCloud Quality Gates
- **Reliability**: Grade must be B or better (strive for A)
- **Maintainability**: Grade must be B or better (strive for A)
- **Code Coverage**: Must not fall below the configured threshold

PRs that worsen grades below B will fail CI and cannot be merged.

### Security Scanning
- **Aikido**: Performs automated security checks
- **Dependabot**: Provides weekly dependency upgrade PRs
- Code must not contain hardcoded credentials, secrets, or sensitive data

### Testing Requirements
- All code must have tests before being merged
- Use **JUnit Jupiter (JUnit 5)** for test execution
- Use **AssertJ** for all assertions (JUnit assertions are forbidden)
- Mockito allowed for mocking; prefer real objects when practical