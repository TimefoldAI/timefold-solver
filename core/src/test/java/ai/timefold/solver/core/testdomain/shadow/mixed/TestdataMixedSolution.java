package ai.timefold.solver.core.testdomain.shadow.mixed;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataMixedSolution {
    @PlanningEntityCollectionProperty
    List<TestdataMixedEntity> mixedEntityList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMixedValue> mixedValueList;

    @ValueRangeProvider
    List<Integer> delayList;

    @PlanningScore
    SimpleScore score;

    public TestdataMixedSolution() {
    }

    public List<TestdataMixedEntity> getMixedEntityList() {
        return mixedEntityList;
    }

    public void setMixedEntityList(List<TestdataMixedEntity> mixedEntityList) {
        this.mixedEntityList = mixedEntityList;
    }

    public List<TestdataMixedValue> getMixedValueList() {
        return mixedValueList;
    }

    public void setMixedValueList(List<TestdataMixedValue> mixedValueList) {
        this.mixedValueList = mixedValueList;
    }

    public List<Integer> getDelayList() {
        return delayList;
    }

    public void setDelayList(List<Integer> delayList) {
        this.delayList = delayList;
    }
}
