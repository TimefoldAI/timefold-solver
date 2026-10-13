package ai.timefold.solver.core.testdomain.shadow.simple_list_next;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataDeclarativeSimpleNextListSolution {

    public static SolutionDescriptor<TestdataDeclarativeSimpleNextListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataDeclarativeSimpleNextListSolution.class,
                TestdataDeclarativeSimpleNextListEntity.class,
                TestdataDeclarativeSimpleNextListValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataDeclarativeSimpleNextListEntity> entityList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDeclarativeSimpleNextListValue> valueList;

    @PlanningScore
    SimpleScore score;

    public TestdataDeclarativeSimpleNextListSolution() {
    }

    public TestdataDeclarativeSimpleNextListSolution(List<TestdataDeclarativeSimpleNextListEntity> entityList,
            List<TestdataDeclarativeSimpleNextListValue> valueList) {
        this.entityList = entityList;
        this.valueList = valueList;
    }

    public List<TestdataDeclarativeSimpleNextListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(
            List<TestdataDeclarativeSimpleNextListEntity> entityList) {
        this.entityList = entityList;
    }

    public List<TestdataDeclarativeSimpleNextListValue> getValueList() {
        return valueList;
    }

    public void setValueList(
            List<TestdataDeclarativeSimpleNextListValue> valueList) {
        this.valueList = valueList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
