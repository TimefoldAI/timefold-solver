package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractLeftDrivenMoveIteratorTest {

    @Test
    void bailOutBudgetDoesNotOverflow() {
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(0)).isZero();
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(3)).isEqualTo(30);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10)).isEqualTo(Long.MAX_VALUE / 10 * 10);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10 + 1)).isEqualTo(Long.MAX_VALUE);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
    }

}
