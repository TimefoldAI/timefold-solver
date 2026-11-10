package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LeftDrivenMoveIteratorTest {

    @Test
    void bailOutBudgetDoesNotOverflow() {
        assertThat(LeftDrivenMoveIterator.bailOutBudget(0)).isZero();
        assertThat(LeftDrivenMoveIterator.bailOutBudget(3)).isEqualTo(30);
        assertThat(LeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10)).isEqualTo(Long.MAX_VALUE / 10 * 10);
        assertThat(LeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10 + 1)).isEqualTo(Long.MAX_VALUE);
        assertThat(LeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
    }

}
