package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;

class NoOpRestartStrategyTest {

    @Test
    void noOp() {
        var strategy = new NoOpRestartStrategy<>();
        assertThat(strategy.isTriggered(any())).isFalse();
    }
}
