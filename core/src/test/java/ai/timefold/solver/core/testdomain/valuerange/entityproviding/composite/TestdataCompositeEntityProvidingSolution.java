package ai.timefold.solver.core.testdomain.valuerange.entityproviding.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataCompositeEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataCompositeEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataCompositeEntityProvidingSolution.class,
                TestdataCompositeEntityProvidingEntity.class);
    }

    public static TestdataCompositeEntityProvidingSolution generateSolution(int valueListSize,
            int entityListSize) {
        var solution = new TestdataCompositeEntityProvidingSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        var entityList = new ArrayList<TestdataCompositeEntityProvidingEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var idx = 0;
            var expectedCount = Math.max(1, valueListSize / 2);
            var valueRange = new ArrayList<TestdataValue>();
            var secondValueRange = new ArrayList<TestdataValue>();
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
            var entity = new TestdataCompositeEntityProvidingEntity("Generated Entity " + i, valueRange,
                    secondValueRange);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    public static TestdataCompositeEntityProvidingSolution generateSolution() {
        var solution = new TestdataCompositeEntityProvidingSolution("s1");
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var value4 = new TestdataValue("v4");
        var value5 = new TestdataValue("v5");
        var entity1 = new TestdataCompositeEntityProvidingEntity("e1", List.of(value1, value2), List.of(value1, value4));
        var entity2 = new TestdataCompositeEntityProvidingEntity("e2", List.of(value1, value3), List.of(value1, value5));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataCompositeEntityProvidingEntity> entityList;

    private SimpleScore score;

    public TestdataCompositeEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataCompositeEntityProvidingSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataCompositeEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataCompositeEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @ProblemFactCollectionProperty
    public Collection<TestdataValue> getProblemFacts() {
        Set<TestdataValue> valueSet = new HashSet<>();
        for (TestdataCompositeEntityProvidingEntity entity : entityList) {
            valueSet.addAll(entity.getValueRange1());
            valueSet.addAll(entity.getValueRange2());
        }
        return valueSet;
    }

}
