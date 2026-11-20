package ai.timefold.solver.core.impl.heuristic.selector.common.nearby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatrixAwareNearbyDistanceMeterTest {

    @Test
    void plainDistanceIsMatrixZero() {
        var meter = new MatrixAwareNearbyDistanceMeter<Object, Integer, Integer>() {

            @Override
            public int initializeDistanceMatrices(Object workingSolution) {
                return 2;
            }

            @Override
            public int getDistanceMatrixIndex(Integer neighbor) {
                return 1;
            }

            @Override
            public double getNearbyDistance(Integer moved, Integer neighbor, int matrix) {
                return (matrix + 1) * (neighbor - moved);
            }
        };

        assertThat(meter.getNearbyDistance(2, 7)).isEqualTo(5.0);
        assertThat(meter.getNearbyDistance(2, 7, 1)).isEqualTo(10.0);
    }

}
