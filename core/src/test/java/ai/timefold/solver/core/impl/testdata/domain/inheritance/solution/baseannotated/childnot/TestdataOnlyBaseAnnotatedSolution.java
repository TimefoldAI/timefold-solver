package ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataOnlyBaseAnnotatedSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    private List<String> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataOnlyBaseAnnotatedBaseEntity> entityList;
    @PlanningScore
    private SimpleScore score;
    private ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<? extends TestdataOnlyBaseAnnotatedBaseEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<? extends TestdataOnlyBaseAnnotatedBaseEntity> entityList) {
        this.entityList = (List<TestdataOnlyBaseAnnotatedBaseEntity>) entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public ConstraintWeightOverrides<SimpleScore> getConstraintWeightOverrides() {
        return constraintWeightOverrides;
    }

    public void setConstraintWeightOverrides(ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }
}
