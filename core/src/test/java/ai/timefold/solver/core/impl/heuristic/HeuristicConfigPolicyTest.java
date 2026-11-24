package ai.timefold.solver.core.impl.heuristic;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.solver.ClassInstanceCache;

import org.junit.jupiter.api.Test;

class HeuristicConfigPolicyTest {

    @Test
    void copyWithOwnClassInstanceCacheKeepsTheOtherSettings() {
        var configPolicy = HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy()
                .cloneBuilder()
                .withUnassignedValuesAllowed(true)
                .withReinitializeVariableFilterEnabled(true)
                .build();
        var classInstanceCache = ClassInstanceCache.create();

        var copy = configPolicy.copyConfigPolicy(classInstanceCache);

        assertThat(copy.getClassInstanceCache())
                .isSameAs(classInstanceCache)
                .isNotSameAs(configPolicy.getClassInstanceCache());
        assertThat(copy.isUnassignedValuesAllowed()).isTrue();
        assertThat(copy.isReinitializeVariableFilterEnabled()).isTrue();
        assertThat(copy.getEntitySorterManner()).isEqualTo(configPolicy.getEntitySorterManner());
        assertThat(copy.getValueSorterManner()).isEqualTo(configPolicy.getValueSorterManner());
    }

}
