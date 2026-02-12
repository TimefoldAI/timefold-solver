package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataAllowsUnassignedMultiVarEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataAllowsUnassignedMultiVarEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataAllowsUnassignedMultiVarEntityProvidingSolution.class,
                TestdataAllowsUnassignedMultiVarEntityProvidingEntity.class);
    }

    public static TestdataAllowsUnassignedMultiVarEntityProvidingSolution generateSolution() {
        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");
        var value3 = new TestdataValue("3");
        var value4 = new TestdataValue("4");
        var value5 = new TestdataValue("5");
        var solution = new TestdataAllowsUnassignedMultiVarEntityProvidingSolution("s1", List.of(value1, value2));
        var entity1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("1", List.of(value1, value2, value3),
                List.of(value1, value2, value4, value5));
        var entity2 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("2", List.of(value1, value2, value3),
                List.of(value1, value2, value4, value5));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    @PlanningEntityCollectionProperty
    private List<TestdataAllowsUnassignedMultiVarEntityProvidingEntity> entityList;
    @PlanningScore
    private SimpleScore score;
    @ValueRangeProvider(id = "solutionValueRange")
    private List<TestdataValue> solutionValueRange;

    public TestdataAllowsUnassignedMultiVarEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataAllowsUnassignedMultiVarEntityProvidingSolution(String code, List<TestdataValue> solutionValueRange) {
        super(code);
        this.solutionValueRange = solutionValueRange;
    }

    public List<TestdataAllowsUnassignedMultiVarEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataAllowsUnassignedMultiVarEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    public List<TestdataValue> getSolutionValueRange() {
        return solutionValueRange;
    }

    public void setSolutionValueRange(List<TestdataValue> solutionValueRange) {
        this.solutionValueRange = solutionValueRange;
    }

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
        for (TestdataAllowsUnassignedMultiVarEntityProvidingEntity entity : entityList) {
            valueSet.addAll(entity.getValueRange());
        }
        return valueSet;
    }

}
