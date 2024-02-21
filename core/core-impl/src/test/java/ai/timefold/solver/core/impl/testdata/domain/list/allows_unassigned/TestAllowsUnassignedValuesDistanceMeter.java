package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

/**
 * For the sake of test readability, planning values (list variable elements) are placed in a 1-dimensional space.
 * An element's coordinate is represented by its ({@link TestdataObject#getCode() code}. If the code is not a number,
 * it is interpreted as zero.
 */
public class TestAllowsUnassignedValuesDistanceMeter
        implements NearbyDistanceMeter<TestdataAllowsUnassignedValuesListValue, TestdataObject> {

    @Override
    public double getNearbyDistance(TestdataAllowsUnassignedValuesListValue origin, TestdataObject destination) {
        return Math.abs(coordinate(destination) - coordinate(origin));
    }

    static int coordinate(TestdataObject o) {
        try {
            return Integer.parseInt(o.getCode());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
