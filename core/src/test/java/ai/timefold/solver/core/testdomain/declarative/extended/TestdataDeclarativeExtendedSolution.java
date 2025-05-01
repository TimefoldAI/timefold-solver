package ai.timefold.solver.core.testdomain.declarative.extended;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataDeclarativeExtendedSolution extends TestdataObject {
    @PlanningEntityCollectionProperty
    List<TestdataDeclarativeExtendedEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDeclarativeExtendedBaseValue> values;

    @PlanningScore
    HardSoftScore score;

    public List<TestdataDeclarativeExtendedEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataDeclarativeExtendedEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataDeclarativeExtendedBaseValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeExtendedBaseValue> values) {
        this.values = values;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
