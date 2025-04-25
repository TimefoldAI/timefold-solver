package ai.timefold.solver.core.testdomain.declarative.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataInvalidDeclarativeSolution extends TestdataObject {
    @PlanningEntityCollectionProperty
    List<TestdataInvalidDeclarativeEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataInvalidDeclarativeValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataInvalidDeclarativeSolution() {

    }

    public TestdataInvalidDeclarativeSolution(String code) {
        super(code);
    }
}
