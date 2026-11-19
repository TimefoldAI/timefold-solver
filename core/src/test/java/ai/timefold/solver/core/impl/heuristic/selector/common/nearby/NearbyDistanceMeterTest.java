package ai.timefold.solver.core.impl.heuristic.selector.common.nearby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NearbyDistanceMeterTest {

    @Test
    void oneMatrixByDefault() {
        NearbyDistanceMeter<Integer, Integer> meter = (moved, neighbor) -> neighbor - moved;

        assertThat(meter.getDistanceMatrixCount()).isOne();
        assertThat(meter.getDistanceMatrixIndex(7)).isZero();
        assertThat(meter.getNearbyDistance(2, 7, 0)).isEqualTo(5.0);
    }

}
