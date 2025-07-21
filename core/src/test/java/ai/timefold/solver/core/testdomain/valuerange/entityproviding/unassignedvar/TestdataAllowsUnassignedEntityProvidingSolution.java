package ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar;

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
public class TestdataAllowsUnassignedEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataAllowsUnassignedEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataAllowsUnassignedEntityProvidingSolution.class,
                TestdataAllowsUnassignedEntityProvidingEntity.class);
    }

    public static TestdataAllowsUnassignedEntityProvidingSolution generateSolution() {
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s1");
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var entity1 = new TestdataAllowsUnassignedEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataAllowsUnassignedEntityProvidingEntity("e2", List.of(value1, value3));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataAllowsUnassignedEntityProvidingEntity> entityList;

    private SimpleScore score;

    public TestdataAllowsUnassignedEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataAllowsUnassignedEntityProvidingSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataAllowsUnassignedEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataAllowsUnassignedEntityProvidingEntity> entityList) {
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
        for (TestdataAllowsUnassignedEntityProvidingEntity entity : entityList) {
            valueSet.addAll(entity.getValueRange());
        }
        return valueSet;
    }

}
