package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * Model with two entity classes:
 * {@link TestdataMultiEntityEntityProvidingEntity} uses an entity value range,
 * and {@link TestdataMultiEntitySolutionRangeEntity} uses only a solution value range.
 */
@PlanningSolution
public class TestdataMultiEntityEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataMultiEntityEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityEntityProvidingSolution.class,
                TestdataMultiEntityEntityProvidingEntity.class, TestdataMultiEntitySolutionRangeEntity.class);
    }

    /**
     * Generates an initialized solution with entityListSize entities of each entity class.
     * The value ranges of the entity-providing entities partially overlap:
     * the entity i accepts the values from i to i + (valueListSize - entityListSize).
     */
    public static TestdataMultiEntityEntityProvidingSolution generateSolution(int valueListSize, int entityListSize) {
        if (valueListSize < entityListSize) {
            throw new IllegalArgumentException("The valueListSize (%d) must be at least the entityListSize (%d)."
                    .formatted(valueListSize, entityListSize));
        }
        var solution = new TestdataMultiEntityEntityProvidingSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
        }
        var valueRangeSize = valueListSize - entityListSize + 1;
        var entityProvidingEntityList = new ArrayList<TestdataMultiEntityEntityProvidingEntity>(entityListSize);
        var solutionRangeEntityList = new ArrayList<TestdataMultiEntitySolutionRangeEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var valueRange = new ArrayList<>(valueList.subList(i, i + valueRangeSize));
            entityProvidingEntityList.add(new TestdataMultiEntityEntityProvidingEntity("Generated Entity " + i,
                    valueRange, valueRange.getFirst()));
            solutionRangeEntityList.add(new TestdataMultiEntitySolutionRangeEntity("Generated Other Entity " + i,
                    valueList.get(i)));
        }
        solution.setValueList(valueList);
        solution.setEntityProvidingEntityList(entityProvidingEntityList);
        solution.setSolutionRangeEntityList(solutionRangeEntityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataMultiEntityEntityProvidingEntity> entityProvidingEntityList;
    private List<TestdataMultiEntitySolutionRangeEntity> solutionRangeEntityList;

    private SimpleScore score;

    public TestdataMultiEntityEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataMultiEntityEntityProvidingSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "solutionValueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataMultiEntityEntityProvidingEntity> getEntityProvidingEntityList() {
        return entityProvidingEntityList;
    }

    public void setEntityProvidingEntityList(List<TestdataMultiEntityEntityProvidingEntity> entityProvidingEntityList) {
        this.entityProvidingEntityList = entityProvidingEntityList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataMultiEntitySolutionRangeEntity> getSolutionRangeEntityList() {
        return solutionRangeEntityList;
    }

    public void setSolutionRangeEntityList(List<TestdataMultiEntitySolutionRangeEntity> solutionRangeEntityList) {
        this.solutionRangeEntityList = solutionRangeEntityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
