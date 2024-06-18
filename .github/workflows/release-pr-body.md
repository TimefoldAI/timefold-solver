At this point, the release of _Timefold Solver Community Edition_ is ready to be published.
Artifacts have been uploaded to a staging repository.
Release branch has been created.

To finish the release of _Timefold Solver_, 
please follow the steps below __in the given order__:

1. [ ] Release the [staging repository on OSSRH](https://s01.oss.sonatype.org/#stagingRepositories).
2. [ ] Wait for the artifacts to [reach Maven Central](https://central.sonatype.com/search?q=ai.timefold.solver&smo=true).
3. [ ] [Undraft the release](https://github.com/TimefoldAI/timefold-solver/releases) on Github. This will cause the release tag to be published, enabling the downstream release scripts.
4. [ ] Merge this PR. (Only do this after undrafting the release, otherwise the tag will point to the final SNAPSHOT commit.)
5. [ ] Delete the branch that this PR is based on. (Typically a button appears on this page once the PR is merged.)
6. [ ] Start and complete release automation for [Timefold Solver Enterprise Edition](https://github.com/TimefoldAI/timefold-solver-enterprise/actions/workflows/release.yml).
7. [ ] Start and complete release automation for [Timefold Solver for Python](https://github.com/TimefoldAI/timefold-solver-python/actions/workflows/release.yml).
8. [ ] Start and complete release automation for [Timefold Solver Enterprise Edition for Python](https://github.com/TimefoldAI/timefold-solver-enterprise-python/actions/workflows/release.yml).
9. [ ] Start and complete release automation for [Timefold Quickstarts](https://github.com/TimefoldAI/timefold-quickstarts).
10. [ ] After finishing all above release automation, run the [website release automation](https://github.com/TimefoldAI/frontend/actions/workflows/solver-release.yml).
11. [ ] Bump Solver version on [start.spring.io](https://start.spring.io) by submitting a PR to [this file](https://github.com/spring-io/start.spring.io/blob/main/start-site/src/main/resources/application.yml).

Note: If this is a dry run, 
none of the above applies and this PR should not be merged.