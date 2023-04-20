package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.config.constructionheuristic.placer.PooledEntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.constructionheuristic.placer.PooledEntityPlacerFactory;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class PooledEntityPlacerFactoryTest {

    @Test
    void unfoldNew() {
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("value"));

        HeuristicConfigPolicy<TestdataSolution> configPolicy = buildHeuristicConfigPolicy();
        PooledEntityPlacerConfig placerConfig =
                PooledEntityPlacerFactory.unfoldNew(configPolicy, moveSelectorConfig);

        assertThat(placerConfig.getMoveSelectorConfig()).isExactlyInstanceOf(ChangeMoveSelectorConfig.class);

        ChangeMoveSelectorConfig changeMoveSelectorConfig =
                (ChangeMoveSelectorConfig) placerConfig.getMoveSelectorConfig();
        assertThat(changeMoveSelectorConfig.getEntitySelectorConfig().getEntityClass()).isNull();
        assertThat(changeMoveSelectorConfig.getEntitySelectorConfig().getMimicSelectorRef())
                .isEqualTo(TestdataEntity.class.getName());
        assertThat(changeMoveSelectorConfig.getValueSelectorConfig().getVariableName()).isEqualTo("value");
    }
}
