package ai.timefold.solver.core.testdomain.shadow.list_element;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataAlignedListElementSolution {

    public static SolutionDescriptor<TestdataAlignedListElementSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataAlignedListElementSolution.class,
                TestdataAlignedListElementEntity.class, TestdataAlignedListElementValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataAlignedListElementEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataAlignedListElementValue> values;

    @PlanningScore
    SimpleScore score;

    public List<TestdataAlignedListElementEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataAlignedListElementEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataAlignedListElementValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataAlignedListElementValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
