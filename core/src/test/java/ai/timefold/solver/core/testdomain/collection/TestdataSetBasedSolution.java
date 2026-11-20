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
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataSetBasedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataSetBasedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataSetBasedSolution.class, TestdataEntity.class);
    }

    private Set<TestdataValue> valueSet;
    private Set<TestdataEntity> entitySet;

    private HashSet<TestdataEntity> entityHashSet;
    private LinkedHashSet<TestdataEntity> entityLinkedHashSet;
    private TreeSet<TestdataEntity> entityTreeSet;

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
    public Set<TestdataEntity> getEntitySet() {
        return entitySet;
    }

    public void setEntitySet(Set<TestdataEntity> entitySet) {
        this.entitySet = entitySet;
    }

    public HashSet<TestdataEntity> getEntityHashSet() {
        return entityHashSet;
    }

    public void setEntityHashSet(HashSet<TestdataEntity> entityHashSet) {
        this.entityHashSet = entityHashSet;
    }

    public LinkedHashSet<TestdataEntity> getEntityLinkedHashSet() {
        return entityLinkedHashSet;
    }

    public void setEntityLinkedHashSet(
            LinkedHashSet<TestdataEntity> entityLinkedHashSet) {
        this.entityLinkedHashSet = entityLinkedHashSet;
    }

    public TreeSet<TestdataEntity> getEntityTreeSet() {
        return entityTreeSet;
    }

    public void setEntityTreeSet(TreeSet<TestdataEntity> entityTreeSet) {
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
