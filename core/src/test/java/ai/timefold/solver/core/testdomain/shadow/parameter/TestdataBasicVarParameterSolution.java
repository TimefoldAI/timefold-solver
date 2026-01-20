package ai.timefold.solver.core.testdomain.shadow.parameter;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataBasicVarParameterSolution {

    public static SolutionDescriptor<TestdataBasicVarParameterSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataBasicVarParameterSolution.class, TestdataBasicVarParameterEntity.class,
                TestdataBasicVarParameterValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataBasicVarParameterEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataBasicVarParameterValue> values;

    @ProblemFactCollectionProperty
    List<Object> problemFacts;

    @PlanningScore
    HardSoftScore score;

    public TestdataBasicVarParameterSolution() {
    }

    public TestdataBasicVarParameterSolution(List<TestdataBasicVarParameterEntity> entities,
            List<TestdataBasicVarParameterValue> values,
            List<Object> problemFacts) {
        this.values = values;
        this.entities = entities;
        this.problemFacts = problemFacts;
    }

    public List<TestdataBasicVarParameterValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataBasicVarParameterValue> values) {
        this.values = values;
    }

    public List<TestdataBasicVarParameterEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataBasicVarParameterEntity> entities) {
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
