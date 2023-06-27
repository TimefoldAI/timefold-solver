# Changelog

{{changelogChanges}}
{{changelogContributors}}

_Timefold Solver Community Edition_ is an open source project, 
and you are more than welcome to contribute as well! 
For more, see [Contributing](https://github.com/TimefoldAI/timefold-solver/blob/main/CONTRIBUTING.adoc).

Should your business need to scale to truly massive data sets or require enterprise-grade support, 
check out [_Timefold Solver Enterprise Edition_](https://github.com/TimefoldAI/timefold-solver-enterprise/releases). 

# How to use Timefold Solver

To see Timefold Solver in action, check out [the quickstarts](https://github.com/TimefoldAI/timefold-quickstarts).

[//]: # (Ideally we'd show the pom.xml snippet, but a JReleaser bug would remove all whitespace from it.)
[//]: # (See https://github.com/jreleaser/jreleaser/issues/1142)
With Maven or Gradle, 
just add the `ai.timefold.solver : timefold-solver-core : {{projectVersion}}` dependency in your `pom.xml` to get started.

You can also import the Timefold Solver Bom (`ai.timefold.solver : timefold-solver-bom : {{projectVersion}}`) 
to avoid duplicating version numbers when adding other Timefold Solver dependencies later on.

# Additional notes

The changelog and the list of contributors above are automatically generated.
They exclude contributions to certain areas of the repository, such as CI and build automation.
This is done for the sake of brevity and to make the user-facing changes stand out more.