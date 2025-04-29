package ai.timefold.solver.core.testdomain.clone.customcloner;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;

@PlanningSolution(solutionCloner = TestdataScoreNotEqualSolution.class)
public class TestdataScoreNotEqualSolution implements SolutionCloner<TestdataScoreNotEqualSolution> {

    @PlanningScore
    private SimpleScore score;
    @PlanningEntityProperty
    private TestdataEntity entity = new TestdataEntity("A");

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> valueRange() {
        return Collections.emptyList();
    }

    @Override
    public @NonNull TestdataScoreNotEqualSolution cloneSolution(@NonNull TestdataScoreNotEqualSolution original) {
        var clone = new TestdataScoreNotEqualSolution();
        clone.entity.setValue(original.entity.getValue());
        if (original.score != null) {
            clone.score = SimpleScore.of(original.score.score() - 1);
        } else {
            clone.score = SimpleScore.ZERO;
        }
        if (clone.score.equals(original.score)) {
            throw new IllegalStateException("The cloned score should be intentionally unequal to the original score");
        }
        return clone;
    }

}
