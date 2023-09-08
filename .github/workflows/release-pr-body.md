At this point, the release of _Timefold Solver Community Edition_ is ready to be published.
Artifacts have been uploaded to a staging repository.
Release branch has been created.

To finish the release of _Timefold Solver Community Edition_, 
please follow the steps below in the given order:

1. [ ] Release the [staging repository on OSSRH](https://s01.oss.sonatype.org/#stagingRepositories).
2. [ ] Wait for the artifacts to [reach Maven Central](https://central.sonatype.com/search?q=ai.timefold.solver&smo=true).
3. [ ] [Undraft the release](https://github.com/TimefoldAI/timefold-solver/releases) on Github. This will cause the release tag to be published, enabling the downstream release scripts.
4. [ ] Merge this PR. (Only do this after undrafting the release, otherwise the tag will point to the final SNAPSHOT commit.)
5. [ ] Delete the branch that this PR is based on. (Typically a button appears on this page once the PR is merged.)
6. [ ] Start release automation for [Timefold Quickstarts](https://github.com/TimefoldAI/timefold-quickstarts).
7. [ ] Start release automation for [Timefold Solver Enterprise Edition](https://github.com/TimefoldAI/timefold-solver-enterprise).

Note: If this is a dry run, 
none of the above applies and this PR should not be merged.