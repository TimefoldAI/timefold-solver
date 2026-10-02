package ai.timefold.solver.core.testdomain.shadow.list_element;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataMixedListElementSolution {

    public static SolutionDescriptor<TestdataMixedListElementSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMixedListElementSolution.class,
                TestdataMixedListElementEntity.class, TestdataMixedListElementValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataMixedListElementEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMixedListElementValue> values;

    @PlanningScore
    SimpleScore score;

    @ValueRangeProvider
    public List<Integer> getDurationRange() {
        return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    public List<TestdataMixedListElementEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataMixedListElementEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataMixedListElementValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataMixedListElementValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
