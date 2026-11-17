package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.util.SelectionUtils;

import org.junit.jupiter.api.Test;

class AbstractLeftDrivenMoveIteratorTest {

    @Test
    void bailOutBudgetIsCapped() {
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(0)).isZero();
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(3)).isEqualTo(30);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10)).isEqualTo(SelectionUtils.MAX_BAIL_OUT);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE)).isEqualTo(SelectionUtils.MAX_BAIL_OUT);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(-1)).isEqualTo(SelectionUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

}
