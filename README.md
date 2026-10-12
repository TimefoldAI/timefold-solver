<p align="center">
  <a href="https://solver.timefold.ai"><img src="docs/src/modules/ROOT/images/shared/timefold-solver-logo.png" alt="Timefold"></a>
</p>

_Planning optimization made easy._  
[timefold.ai](https://timefold.ai)

[![GitHub Discussions](https://img.shields.io/github/discussions/TimefoldAI/timefold-solver?style=for-the-badge&logo=github)](https://github.com/TimefoldAI/timefold-solver/discussions)
[![Community Discord](https://img.shields.io/discord/1413420192213631086?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/bW8tUUeBzH)
[![Commit Activity](https://img.shields.io/github/commit-activity/m/TimefoldAI/timefold-solver?label=commits&style=for-the-badge)](https://github.com/TimefoldAI/timefold-solver/pulse)
[![GitHub Issues](https://img.shields.io/github/issues/TimefoldAI/timefold-solver?style=for-the-badge&logo=github)](https://github.com/TimefoldAI/timefold-solver/issues)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ai.timefold:timefold-solver&style=for-the-badge&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=ai.timefold:timefold-solver)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ai.timefold:timefold-solver&metric=security_rating)](https://sonarcloud.io/dashboard?id=ai.timefold:timefold-solver)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ai.timefold:timefold-solver&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=ai.timefold:timefold-solver)

Timefold Solver is an AI constraint solver for Java and Kotlin.
You can use Timefold Solver to optimize the Vehicle Routing Problem, Employee Rostering,
Maintenance Scheduling, Task Assignment, School Timetabling, Cloud Optimization,
Conference Scheduling, Job Shop Scheduling and many more planning problems.

Developed by the original OptaPlanner team, our aim is to free the world of wasteful planning.

## Get started with Timefold Solver in Java

[![Maven artifact](https://img.shields.io/maven-central/v/ai.timefold.solver/timefold-solver-bom?logo=apache-maven&style=for-the-badge)](https://ossindex.sonatype.org/component/pkg:maven/ai.timefold.solver/timefold-solver-bom)
[![JVM support](https://img.shields.io/badge/Java-21+-brightgreen.svg?style=for-the-badge)](https://sdkman.io)

* [Read a Getting Started guide.](https://docs.timefold.ai/timefold-solver/latest/quickstart/overview)
* [Clone the Quickstarts repository.](https://github.com/TimefoldAI/timefold-quickstarts)

## Build from source

1. Install JDK 21+ and Maven 3.9.11+, for example with [Sdkman](https://sdkman.io):

   ```
   $ sdk install java
   $ sdk install maven
   ```

2. Git clone this repository:

   ```
   $ git clone https://github.com/TimefoldAI/timefold-solver.git
   $ cd timefold-solver
   ```

3. Build it from source:

   ```
   $ ./mvnw clean install -Dquickly
   ```

## Contribute

This is an open source project, and you are more than welcome to contribute!
For more, see [Contributing](CONTRIBUTING.md).

## Editions

There are two editions of Timefold Solver:

- _Timefold Solver Community Edition_,
- _Timefold Solver Enterprise_.

The Community Edition (this repo) is open-source and licensed under the Apache-2.0 license.
The Enterprise Edition is a non-open-source commercial offering and requires a Timefold license to run.

[See which edition is right for you.](https://licenses.timefold.ai/)

## Legal notice

Timefold Solver was [forked](https://timefold.ai/blog/2023/optaplanner-fork/) on 20 April 2023 from OptaPlanner,
which was entirely Apache-2.0 licensed (a permissive license).

Timefold Solver is a derivative work of OptaPlanner and OptaPy,
which includes copyrights of the original creator, Red Hat Inc., affiliates, and contributors,
that were all entirely licensed under the Apache-2.0 license.
Every source file has been modified.

### Documentation icon libraries

- [tabler icons](https://tabler.io/icons) under [MIT license](https://docs.tabler.io/ui/getting-started/license)
