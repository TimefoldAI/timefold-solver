package ai.timefold.solver.core.preview.api.variable.provided;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

@PlanningSolution
public class RoutePlan {
    @PlanningEntityCollectionProperty
    List<Vehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<Visit> visits;

    @PlanningScore
    HardSoftScore score;
}
