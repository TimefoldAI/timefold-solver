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

@PlanningSolution(solutionCloner = TestdataEntitiesNotClonedSolution.class)
public class TestdataEntitiesNotClonedSolution implements SolutionCloner<TestdataEntitiesNotClonedSolution> {

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
    public @NonNull TestdataEntitiesNotClonedSolution cloneSolution(@NonNull TestdataEntitiesNotClonedSolution original) {
        TestdataEntitiesNotClonedSolution clone = new TestdataEntitiesNotClonedSolution();
        clone.entity = original.entity;
        clone.score = original.score;
        return clone;
    }

}
