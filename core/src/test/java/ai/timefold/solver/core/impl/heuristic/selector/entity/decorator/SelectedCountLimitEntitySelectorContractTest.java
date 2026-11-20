package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;

import org.junit.jupiter.api.Test;

class SelectedCountLimitEntitySelectorContractTest {

    private static final TestdataEntity E1 = new TestdataEntity("e1");
    private static final TestdataEntity E2 = new TestdataEntity("e2");
    private static final TestdataEntity E3 = new TestdataEntity("e3");

    @Test
    void listIteratorRespectsLimit() {
        var child = SelectorTestUtils.mockEntitySelector(TestdataEntity.class, E1, E2, E3);
        var selector = new SelectedCountLimitEntitySelector<>(child, false, 2L);

        var iterator = selector.listIterator();
        assertThat(iterator.next()).isSameAs(E1);
        assertThat(iterator.next()).isSameAs(E2);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void listIteratorWithIndexRespectsLimit() {
        var child = SelectorTestUtils.mockEntitySelector(TestdataEntity.class, E1, E2, E3);
        var selector = new SelectedCountLimitEntitySelector<>(child, false, 2L);

        var iterator = selector.listIterator(1);
        assertThat(iterator.next()).isSameAs(E2);
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.hasPrevious()).isTrue();
        assertThat(iterator.previous()).isSameAs(E2);
        assertThat(iterator.previous()).isSameAs(E1);
    }

    @Test
    void listIteratorFailsFastOnRandomSelection() {
        var child = SelectorTestUtils.mockEntitySelector(TestdataEntity.class, E1, E2, E3);
        var selector = new SelectedCountLimitEntitySelector<>(child, true, 2L);

        assertThatThrownBy(selector::listIterator)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("randomSelection");
    }
}
