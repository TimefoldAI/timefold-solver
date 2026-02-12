package ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataListUnassignedEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListUnassignedEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListUnassignedEntityProvidingSolution.class,
                TestdataListUnassignedEntityProvidingEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataListUnassignedEntityProvidingSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataListUnassignedEntityProvidingSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        var entityList = new ArrayList<TestdataListUnassignedEntityProvidingEntity>(entityListSize);
        var idx = 0;
        for (var i = 0; i < entityListSize; i++) {
            var expectedCount = Math.max(1, valueListSize / entityListSize);
            var valueRange = new ArrayList<TestdataValue>();
            for (var j = 0; j < expectedCount; j++) {
                if (idx >= valueListSize) {
                    break;
                }
                valueRange.add(valueList.get(idx++));
            }
            var entity = new TestdataListUnassignedEntityProvidingEntity("Generated Entity " + i, valueRange);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    public static TestdataListUnassignedEntityProvidingSolution generateSolution() {
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var entity1 = new TestdataListUnassignedEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataListUnassignedEntityProvidingEntity("e2", List.of(value1, value3));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataListUnassignedEntityProvidingEntity> entityList;

    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListUnassignedEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListUnassignedEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return entityList.stream()
                .flatMap(entity -> entity.getValueRange().stream())
                .distinct()
                .toList();
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
