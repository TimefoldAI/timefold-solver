package ai.timefold.solver.core.testdomain.list.shadowhistory;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListSolutionWithShadowHistory {

    public static SolutionDescriptor<TestdataListSolutionWithShadowHistory> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSolutionWithShadowHistory.class,
                TestdataListEntityWithShadowHistory.class,
                TestdataListValueWithShadowHistory.class);
    }

    private List<TestdataListValueWithShadowHistory> valueList;
    private List<TestdataListEntityWithShadowHistory> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataListValueWithShadowHistory> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListValueWithShadowHistory> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListEntityWithShadowHistory> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListEntityWithShadowHistory> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
