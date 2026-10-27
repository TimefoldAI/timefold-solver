package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multientity;

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
