package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.testdomain.shadow.fact_group_source.TestdataFactGroupSourceSolution;
import ai.timefold.solver.core.testdomain.shadow.fact_group_source.TestdataFactGroupSourceVehicle;
import ai.timefold.solver.core.testdomain.shadow.fact_group_source.TestdataFactGroupSourceVisit;

import org.junit.jupiter.api.Test;

class FactGroupSourceShadowVariableTest {

    @Test
    void computeVariableWithoutAnySource() {
        var vehicle = new TestdataFactGroupSourceVehicle("A");
        var visit = new TestdataFactGroupSourceVisit("a");
        vehicle.setVisits(List.of(visit));
        var solution = new TestdataFactGroupSourceSolution();
        solution.setVehicles(List.of(vehicle));
        solution.setVisits(List.of(visit));

        SolutionManager.updateShadowVariables(solution);

        assertThat(vehicle.getStartTime()).isZero();
        assertThat(visit.getStartTime()).isZero();
    }
}
