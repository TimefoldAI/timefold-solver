package ai.timefold.solver.core.testdomain.shadow.invalid.parameter;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataInvalidDeclarativeParameterSolution extends TestdataObject {
    @PlanningEntityCollectionProperty
    List<TestdataInvalidDeclarativeParameterEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataInvalidDeclarativeParameterValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataInvalidDeclarativeParameterSolution() {

    }

    public TestdataInvalidDeclarativeParameterSolution(String code) {
        super(code);
    }

    public List<TestdataInvalidDeclarativeParameterEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataInvalidDeclarativeParameterEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataInvalidDeclarativeParameterValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataInvalidDeclarativeParameterValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
