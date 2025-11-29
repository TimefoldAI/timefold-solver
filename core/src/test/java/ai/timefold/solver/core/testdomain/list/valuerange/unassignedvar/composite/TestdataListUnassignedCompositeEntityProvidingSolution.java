package ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.composite;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

@PlanningSolution
public class TestdataListUnassignedCompositeEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListUnassignedCompositeEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListUnassignedCompositeEntityProvidingSolution.class,
                TestdataListUnassignedCompositeEntityProvidingEntity.class);
    }

    public static TestdataListUnassignedCompositeEntityProvidingSolution generateSolution(int valueListSize,
            int entityListSize) {
        var solution = new TestdataListUnassignedCompositeEntityProvidingSolution();
        var valueList = new ArrayList<TestdataListEntityProvidingValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataListEntityProvidingValue("Generated Value " + i);
            valueList.add(value);
        }
        var entityList = new ArrayList<TestdataListUnassignedCompositeEntityProvidingEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var idx = 0;
            var expectedCount = Math.max(1, valueListSize / 2);
            var valueRange = new ArrayList<TestdataListEntityProvidingValue>();
            var secondValueRange = new ArrayList<TestdataListEntityProvidingValue>();
            for (var j = 0; j < expectedCount; j++) {
                if (idx >= valueListSize) {
                    break;
                }
                valueRange.add(valueList.get(idx++));
            }
            for (var j = 0; j < expectedCount; j++) {
                if (idx >= valueListSize) {
                    break;
                }
                secondValueRange.add(valueList.get(idx++));
            }
            var entity = new TestdataListUnassignedCompositeEntityProvidingEntity("Generated Entity " + i, valueRange,
                    secondValueRange);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    public static TestdataListUnassignedCompositeEntityProvidingSolution generateSolution() {
        var solution = new TestdataListUnassignedCompositeEntityProvidingSolution();
        var value1 = new TestdataListEntityProvidingValue("v1");
        var value2 = new TestdataListEntityProvidingValue("v2");
        var value3 = new TestdataListEntityProvidingValue("v3");
        var value4 = new TestdataListEntityProvidingValue("v4");
        var value5 = new TestdataListEntityProvidingValue("v5");
        var entity1 = new TestdataListUnassignedCompositeEntityProvidingEntity("e1", List.of(value1, value2),
                List.of(value1, value3));
        var entity2 = new TestdataListUnassignedCompositeEntityProvidingEntity("e2", List.of(value1, value4),
                List.of(value1, value5));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataListUnassignedCompositeEntityProvidingEntity> entityList;
    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListUnassignedCompositeEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListUnassignedCompositeEntityProvidingEntity> entityList) {
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
