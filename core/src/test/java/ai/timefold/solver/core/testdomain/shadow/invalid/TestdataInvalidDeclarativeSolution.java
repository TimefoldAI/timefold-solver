package ai.timefold.solver.core.testdomain.shadow.invalid;

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

    public List<TestdataInvalidDeclarativeEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataInvalidDeclarativeEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataInvalidDeclarativeValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataInvalidDeclarativeValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
