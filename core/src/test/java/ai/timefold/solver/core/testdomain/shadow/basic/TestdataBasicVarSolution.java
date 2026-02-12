package ai.timefold.solver.core.testdomain.shadow.basic;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataBasicVarSolution {

    public static SolutionDescriptor<TestdataBasicVarSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataBasicVarSolution.class, TestdataBasicVarEntity.class, TestdataBasicVarValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataBasicVarEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataBasicVarValue> values;

    @ProblemFactCollectionProperty
    List<Object> problemFacts;

    @PlanningScore
    HardSoftScore score;

    public TestdataBasicVarSolution() {
    }

    public TestdataBasicVarSolution(List<TestdataBasicVarEntity> entities, List<TestdataBasicVarValue> values,
            List<Object> problemFacts) {
        this.values = values;
        this.entities = entities;
        this.problemFacts = problemFacts;
    }

    public List<TestdataBasicVarValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataBasicVarValue> values) {
        this.values = values;
    }

    public List<TestdataBasicVarEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataBasicVarEntity> entities) {
        this.entities = entities;
    }

    public List<Object> getProblemFacts() {
        return problemFacts;
    }

    public void setProblemFacts(List<Object> problemFacts) {
        this.problemFacts = problemFacts;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "TestdataBasicVarSolution{" +
                "entities=" + entities +
                ", values=" + values +
                ", problemFacts=" + problemFacts +
                ", score=" + score +
                '}';
    }
}
