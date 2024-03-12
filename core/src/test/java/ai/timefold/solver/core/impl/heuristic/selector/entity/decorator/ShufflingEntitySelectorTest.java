package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;

import org.junit.jupiter.api.Test;

class ShufflingEntitySelectorTest {

    @Test
    void isNeverEnding() {
        ShufflingEntitySelector selector = new ShufflingEntitySelector(mock(EntitySelector.class), SelectionCacheType.PHASE);
        assertThat(selector.isNeverEnding()).isFalse();
    }

    @Test
    void isCountable() {
        ShufflingEntitySelector selector = new ShufflingEntitySelector(mock(EntitySelector.class), SelectionCacheType.PHASE);
        assertThat(selector.isCountable()).isTrue();
    }

}
