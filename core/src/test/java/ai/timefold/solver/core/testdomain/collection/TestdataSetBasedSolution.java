package ai.timefold.solver.core.testdomain.collection;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataSetBasedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataSetBasedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataSetBasedSolution.class, TestdataSetBasedEntity.class);
    }

    private Set<TestdataValue> valueSet;
    private Set<TestdataSetBasedEntity> entitySet;

    private HashSet<TestdataSetBasedEntity> entityHashSet;
    private LinkedHashSet<TestdataSetBasedEntity> entityLinkedHashSet;
    private TreeSet<TestdataSetBasedEntity> entityTreeSet;

    private SimpleScore score;

    public TestdataSetBasedSolution() {
    }

    public TestdataSetBasedSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public Set<TestdataValue> getValueSet() {
        return valueSet;
    }

    public void setValueSet(Set<TestdataValue> valueSet) {
        this.valueSet = valueSet;
    }

    @PlanningEntityCollectionProperty
    public Set<TestdataSetBasedEntity> getEntitySet() {
        return entitySet;
    }

    public void setEntitySet(Set<TestdataSetBasedEntity> entitySet) {
        this.entitySet = entitySet;
    }

    public HashSet<TestdataSetBasedEntity> getEntityHashSet() {
        return entityHashSet;
    }

    public void setEntityHashSet(HashSet<TestdataSetBasedEntity> entityHashSet) {
        this.entityHashSet = entityHashSet;
    }

    public LinkedHashSet<TestdataSetBasedEntity> getEntityLinkedHashSet() {
        return entityLinkedHashSet;
    }

    public void setEntityLinkedHashSet(
            LinkedHashSet<TestdataSetBasedEntity> entityLinkedHashSet) {
        this.entityLinkedHashSet = entityLinkedHashSet;
    }

    public TreeSet<TestdataSetBasedEntity> getEntityTreeSet() {
        return entityTreeSet;
    }

    public void setEntityTreeSet(TreeSet<TestdataSetBasedEntity> entityTreeSet) {
        this.entityTreeSet = entityTreeSet;
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

}
