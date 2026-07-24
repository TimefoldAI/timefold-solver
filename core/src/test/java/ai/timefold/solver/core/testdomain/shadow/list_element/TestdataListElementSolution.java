package ai.timefold.solver.core.testdomain.shadow.list_element;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataListElementSolution {

    public static SolutionDescriptor<TestdataListElementSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListElementSolution.class,
                TestdataListElementEntity.class, TestdataListElementValue.class);
    }

    public static PlanningSolutionMetaModel<TestdataListElementSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataListElementSolution generateSolution(int entityCount, int valueCount) {
        var solution = new TestdataListElementSolution();
        var entities = new ArrayList<TestdataListElementEntity>(entityCount);
        for (var i = 0; i < entityCount; i++) {
            entities.add(new TestdataListElementEntity("e" + i, i));
        }
        var values = new ArrayList<TestdataListElementValue>(valueCount);
        for (var i = 0; i < valueCount; i++) {
            values.add(new TestdataListElementValue("v" + i, 1 + (i % 3)));
        }
        solution.setEntities(entities);
        solution.setValues(values);
        return solution;
    }

    @PlanningEntityCollectionProperty
    List<TestdataListElementEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataListElementValue> values;

    @PlanningScore
    SimpleScore score;

    public List<TestdataListElementEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataListElementEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataListElementValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataListElementValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
