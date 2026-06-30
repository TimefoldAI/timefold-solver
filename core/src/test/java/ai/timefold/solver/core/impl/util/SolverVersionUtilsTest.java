package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;

import org.junit.jupiter.api.Test;

class SolverVersionUtilsTest {

    @Test
    void stripReleaseCandidate_stripsTrailingRcSuffix() {
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3-rc-2")).isEqualTo("1.2.3");
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3-rc-10")).isEqualTo("1.2.3");
    }

    @Test
    void stripReleaseCandidate_leavesNonRcVersionsUnchanged() {
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3")).isEqualTo("1.2.3");
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3-beta-4")).isEqualTo("1.2.3-beta-4");
        assertThat(SolverVersionUtils.stripReleaseCandidate("999-SNAPSHOT")).isEqualTo("999-SNAPSHOT");
        // non-standard spellings pass through verbatim
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3-RC-2")).isEqualTo("1.2.3-RC-2");
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3-rc.2")).isEqualTo("1.2.3-rc.2");
        assertThat(SolverVersionUtils.stripReleaseCandidate("1.2.3-rc")).isEqualTo("1.2.3-rc");
    }

    @Test
    void stripReleaseCandidate_handlesNull() {
        assertThat(SolverVersionUtils.stripReleaseCandidate(null)).isNull();
    }

    @Test
    void banner_withVersion() {
        assertThat(SolverVersionUtils.banner("My Edition", SolverVersionUtilsTest.class))
                // SolverVersionUtilsTest has no manifest version, so falls back to DEVELOPMENT_SNAPSHOT
                .isEqualTo("My Edition " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT);
    }

    @Test
    void communityBannerWithGitRef_withRef() {
        assertThat(SolverVersionUtils.communityBannerWithGitRef(SolverVersionUtilsTest.class, "a1b2c3d"))
                .isEqualTo(SolverVersionUtils.COMMUNITY_NAME + " " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT + " (a1b2c3d)");
    }

    @Test
    void communityBannerWithGitRef_withoutRef() {
        assertThat(SolverVersionUtils.communityBannerWithGitRef(SolverVersionUtilsTest.class, null))
                .isEqualTo(SolverVersionUtils.COMMUNITY_NAME + " " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT);
    }

    @Test
    void enterpriseBannerWithGitRef_bothRefs() {
        assertThat(SolverVersionUtils.enterpriseBannerWithGitRef(SolverVersionUtilsTest.class, "aaa1111", "bbb2222"))
                .isEqualTo(SolverVersionUtils.ENTERPRISE_NAME + " " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT
                        + " (core aaa1111, enterprise bbb2222)");
    }

    @Test
    void enterpriseBannerWithGitRef_onlyCoreRef() {
        assertThat(SolverVersionUtils.enterpriseBannerWithGitRef(SolverVersionUtilsTest.class, "aaa1111", null))
                .isEqualTo(SolverVersionUtils.ENTERPRISE_NAME + " " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT
                        + " (core aaa1111)");
    }

    @Test
    void enterpriseBannerWithGitRef_onlyEnterpriseRef() {
        assertThat(SolverVersionUtils.enterpriseBannerWithGitRef(SolverVersionUtilsTest.class, null, "bbb2222"))
                .isEqualTo(SolverVersionUtils.ENTERPRISE_NAME + " " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT
                        + " (enterprise bbb2222)");
    }

    @Test
    void enterpriseBannerWithGitRef_noRefs() {
        assertThat(SolverVersionUtils.enterpriseBannerWithGitRef(SolverVersionUtilsTest.class, null, null))
                .isEqualTo(SolverVersionUtils.ENTERPRISE_NAME + " " + SolverVersionUtils.DEVELOPMENT_SNAPSHOT);
    }

    @Test
    void gitRefOf_corePropertiesIsNullOrValidSha() {
        var ref = SolverVersionUtils.gitRefOf(SolverVersionUtils.CORE_GIT_PROPERTIES);
        if (ref != null) {
            assertThat(ref).matches("^[0-9a-f]{7,40}$");
        }
        // null is acceptable (IDE without Maven, no .git directory)
    }

    @Test
    void identifySolverVersionWithGitRef_matchesCommunityPattern() {
        assertThat(TimefoldSolverEnterpriseService.identifySolverVersionWithGitRef())
                .matches("^Timefold Solver Community Edition .+$");
    }

}
