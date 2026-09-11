package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.solveWithFullAssert;

import java.util.ArrayList;

import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListElementCascadeVariableReferenceGraph} on a next-directional model,
 * where a vehicle's latest start time is bound by its successor vehicles
 * and propagation walks each route backwards.
 */
class ListElementCascadeNextShadowVariableTest {

    @Test
    void solveNextDirectionalModelWithFullAssert() {
        var vehicles = new ArrayList<TestdataMultiEntityChainNextVehicle>();
        for (var i = 0; i < 3; i++) {
            vehicles.add(new TestdataMultiEntityChainNextVehicle("vehicle" + i, 100));
        }
        // vehicle0 -> vehicle1 -> vehicle2 chain.
        vehicles.get(0).getNextVehicles().add(vehicles.get(1));
        vehicles.get(1).getNextVehicles().add(vehicles.get(2));
        var visits = new ArrayList<TestdataMultiEntityChainNextVisit>();
        for (var i = 0; i < 6; i++) {
            visits.add(new TestdataMultiEntityChainNextVisit("visit" + i, 1 + (i % 3)));
        }
        var problem = new TestdataMultiEntityChainNextSolution();
        problem.setVehicles(vehicles);
        problem.setVisits(visits);

        assertShadowsAreAtFixedPoint(solveWithFullAssert(TestdataMultiEntityChainNextSolution.class,
                TestdataMultiEntityChainNextConstraintProvider.class, problem,
                TestdataMultiEntityChainNextVehicle.class, TestdataMultiEntityChainNextVisit.class));
    }

    private static void assertShadowsAreAtFixedPoint(TestdataMultiEntityChainNextSolution solution) {
        DeclarativeShadowVariableAssertions.assertShadowsAreAtFixedPoint(solution,
                s -> s.getVehicles().stream().map(TestdataMultiEntityChainNextVehicle::getStartTime).toList(),
                s -> s.getVisits().stream().map(TestdataMultiEntityChainNextVisit::getLatestStartTime).toList());
    }
}
