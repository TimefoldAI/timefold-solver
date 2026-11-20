package ai.timefold.solver.core.impl.neighborhood.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class DefaultNeighborhoodTest {

    @Test
    void rejectsEmptyMoveProviderList() {
        assertThatThrownBy(() -> new DefaultNeighborhood<TestdataSolution>(new LinkedHashMap<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one move provider");
    }

    @Test
    void acceptsNonEmptyMoveProviderList() {
        MoveProvider<TestdataSolution> moveProvider = moveStreamFactory -> {
            throw new UnsupportedOperationException(); // The test will not get here.
        };
        var neighborhood = new DefaultNeighborhood<TestdataSolution>(new LinkedHashMap<>(Map.of("id", moveProvider)));
        assertThat(neighborhood.moveProviderMap().values()).containsExactly(moveProvider);
    }

}
