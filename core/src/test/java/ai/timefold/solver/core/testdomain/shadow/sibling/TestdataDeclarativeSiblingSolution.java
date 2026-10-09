package ai.timefold.solver.core.testdomain.shadow.sibling;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataDeclarativeSiblingSolution extends TestdataObject {
    @PlanningEntityCollectionProperty
    List<TestdataDeclarativeSiblingEntity> entities;
    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<AbstractTestdataDeclarativeSiblingValue> values;
    @PlanningScore
    SimpleScore score;

    public List<TestdataDeclarativeSiblingEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataDeclarativeSiblingEntity> entities) {
        this.entities = entities;
    }

    public List<AbstractTestdataDeclarativeSiblingValue> getValues() {
        return values;
    }

    public void setValues(List<AbstractTestdataDeclarativeSiblingValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
