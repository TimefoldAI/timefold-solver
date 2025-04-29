
package ai.timefold.solver.core.testdomain.immutable.enumeration;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataEnumSolution {

    public static SolutionDescriptor<TestdataEnumSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEnumSolution.class,
                TestdataEnumEntity.class);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    List<TestdataEnumValue> valueList;
    @PlanningEntityCollectionProperty
    List<TestdataEnumEntity> entityList;
    @PlanningScore
    SimpleScore score;

    public List<TestdataEnumValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataEnumValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataEnumEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEnumEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
