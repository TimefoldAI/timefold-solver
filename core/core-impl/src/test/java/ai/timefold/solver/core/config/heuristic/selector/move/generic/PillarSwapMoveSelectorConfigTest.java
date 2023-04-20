package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;

import org.junit.jupiter.api.Test;

class PillarSwapMoveSelectorConfigTest {

    @Test
    void withMethodCallsProperlyChain() {
        final int minimumSubPillarSize = 5;
        final double fixedProbabilityWeight = 1.0;
        final int maximumSubPillarSize = 10;
        PillarSwapMoveSelectorConfig pillarSwapMoveSelectorConfig = new PillarSwapMoveSelectorConfig()
                .withPillarSelectorConfig(new PillarSelectorConfig().withMinimumSubPillarSize(minimumSubPillarSize))
                .withFixedProbabilityWeight(fixedProbabilityWeight)
                .withSecondaryPillarSelectorConfig(
                        new PillarSelectorConfig().withMaximumSubPillarSize(maximumSubPillarSize));

        assertSoftly(softly -> {
            softly.assertThat(pillarSwapMoveSelectorConfig.getFixedProbabilityWeight()).isEqualTo(fixedProbabilityWeight);
            softly.assertThat(pillarSwapMoveSelectorConfig.getPillarSelectorConfig()).isNotNull();
            softly.assertThat(pillarSwapMoveSelectorConfig.getPillarSelectorConfig().getMinimumSubPillarSize())
                    .isEqualTo(minimumSubPillarSize);
            softly.assertThat(pillarSwapMoveSelectorConfig.getSecondaryPillarSelectorConfig()).isNotNull();
            softly.assertThat(pillarSwapMoveSelectorConfig.getSecondaryPillarSelectorConfig().getMaximumSubPillarSize())
                    .isEqualTo(maximumSubPillarSize);
        });
    }
}
