package ai.timefold.solver.core.testdomain.valuerange.entityproviding;

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
public class TestdataEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEntityProvidingSolution.class,
                TestdataEntityProvidingEntity.class);
    }

    public static TestdataEntityProvidingSolution generateSolution() {
        var solution = new TestdataEntityProvidingSolution("s1");
        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");
        var value3 = new TestdataValue("3");
        var entity1 = new TestdataEntityProvidingEntity("1", List.of(value1, value2));
        var entity2 = new TestdataEntityProvidingEntity("2", List.of(value1, value3));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    public static TestdataEntityProvidingSolution generateSolution(int valueListSize, int entityListSize) {
        return generateSolution(valueListSize, entityListSize, true);
    }

    public static TestdataEntityProvidingSolution generateUninitializedSolution(int valueListSize, int entityListSize) {
        return generateSolution(valueListSize, entityListSize, false);
    }

    private static TestdataEntityProvidingSolution generateSolution(int valueListSize, int entityListSize,
            boolean initialized) {
        var solution = new TestdataEntityProvidingSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        var entityList = new ArrayList<TestdataEntityProvidingEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var expectedCount = Math.max(1, valueListSize / entityListSize);
            var valueRange = new ArrayList<TestdataValue>();
            for (var j = 0; j < expectedCount; j++) {
                valueRange.add(valueList.get((i * j) % valueListSize));
            }
            var entity = new TestdataEntityProvidingEntity("Generated Entity " + i, valueRange);
            entity.setValue(initialized ? valueList.get(i % valueListSize) : null);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataEntityProvidingEntity> entityList;

    private SimpleScore score;

    public TestdataEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataEntityProvidingSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntityProvidingEntity> entityList) {
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
        for (TestdataEntityProvidingEntity entity : entityList) {
            valueSet.addAll(entity.getValueRange());
        }
        return valueSet;
    }

}
