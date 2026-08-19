package ai.timefold.solver.core.impl.neighborhood.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class DefaultNeighborhoodTest {

    @Test
    void rejectsEmptyMoveProviderList() {
        assertThatThrownBy(() -> new DefaultNeighborhood<TestdataSolution>(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one move provider");
    }

    @Test
    void acceptsNonEmptyMoveProviderList() {
        MoveProvider<TestdataSolution> moveProvider = moveStreamFactory -> {
            throw new UnsupportedOperationException(); // The test will not get here.
        };
        var neighborhood = new DefaultNeighborhood<TestdataSolution>(List.of(moveProvider));
        assertThat(neighborhood.getMoveProviderList()).containsExactly(moveProvider);
    }

}
