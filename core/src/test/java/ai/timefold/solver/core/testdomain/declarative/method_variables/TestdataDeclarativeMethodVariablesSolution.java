package ai.timefold.solver.core.testdomain.declarative.method_variables;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataDeclarativeMethodVariablesSolution extends TestdataObject {
    @PlanningEntityCollectionProperty
    List<TestdataDeclarativeMethodVariablesEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDeclarativeMethodVariablesBaseValue> values;

    @PlanningScore
    HardSoftScore score;

    public List<TestdataDeclarativeMethodVariablesEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataDeclarativeMethodVariablesEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataDeclarativeMethodVariablesBaseValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeMethodVariablesBaseValue> values) {
        this.values = values;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
