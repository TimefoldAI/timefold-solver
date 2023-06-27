At this point, the release of _Timefold Solver Community Edition_ is ready to be published.
Artifacts have been uploaded to OSSRH.
Release branch has been created.
Git tag has been published.

To finish the release of _Timefold Solver Community Edition_, 
please follow the steps below in the given order:

- [ ] Release the [staging repository on OSSRH](https://s01.oss.sonatype.org/#stagingRepositories).
- [ ] Start release automation for [Timefold Quickstarts](https://github.com/TimefoldAI/timefold-quickstarts).
- [ ] Start release automation for [Timefold Solver Enterprise Edition](https://github.com/TimefoldAI/timefold-solver-enterprise).
- [ ] Wait for the artifacts to [reach Maven Central](https://central.sonatype.com/search?q=ai.timefold.solver&smo=true).
- [ ] [Undraft the release](https://github.com/TimefoldAI/timefold-solver/releases) on Github.
- [ ] Merge this PR.
- [ ] Delete the branch that this PR is based on. (Typically a button appears on this page once the PR is merged.)

Note: If this is a dry run, 
none of the above applies and this PR should not be merged.