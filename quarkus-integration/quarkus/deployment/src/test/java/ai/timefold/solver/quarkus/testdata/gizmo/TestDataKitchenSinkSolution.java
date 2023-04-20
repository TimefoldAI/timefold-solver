package ai.timefold.solver.quarkus.testdata.gizmo;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

@PlanningSolution
public class TestDataKitchenSinkSolution {

    @PlanningEntityProperty
    private TestDataKitchenSinkEntity planningEntityProperty;

    @PlanningEntityCollectionProperty
    private List<TestDataKitchenSinkEntity> planningEntityListProperty;

    @ProblemFactProperty
    private String problemFactProperty;

    @ProblemFactCollectionProperty
    private List<String> problemFactListProperty;

    @PlanningScore
    private HardSoftLongScore score;

    public TestDataKitchenSinkSolution() {

    }

    public TestDataKitchenSinkSolution(TestDataKitchenSinkEntity planningEntityProperty,
            List<TestDataKitchenSinkEntity> planningEntityListProperty, String problemFactProperty,
            List<String> problemFactListProperty, HardSoftLongScore score) {
        this.planningEntityProperty = planningEntityProperty;
        this.planningEntityListProperty = planningEntityListProperty;
        this.problemFactProperty = problemFactProperty;
        this.problemFactListProperty = problemFactListProperty;
        this.score = score;
    }

    public TestDataKitchenSinkEntity getPlanningEntityProperty() {
        return planningEntityProperty;
    }
}
