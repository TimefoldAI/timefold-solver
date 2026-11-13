package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.list;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataListInheritanceSolution {

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataValue> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataListInheritanceBaseEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public TestdataListInheritanceSolution() {
    }

    public TestdataListInheritanceSolution(List<TestdataValue> valueList, List<TestdataListInheritanceBaseEntity> entityList) {
        this.valueList = valueList;
        this.entityList = entityList;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListInheritanceBaseEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListInheritanceBaseEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
