package ai.timefold.solver.quarkus.testdomain.gizmo;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.HardSoftScore;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.FIELD)
public class TestDataKitchenSinkAutoDiscoverFieldSolution {

    private TestDataKitchenSinkEntity planningEntityProperty;
    private List<TestDataKitchenSinkEntity> planningEntityListProperty;
    private String problemFactProperty;
    private List<String> problemFactListProperty;
    private HardSoftScore score;

    public TestDataKitchenSinkAutoDiscoverFieldSolution() {

    }

    public TestDataKitchenSinkAutoDiscoverFieldSolution(TestDataKitchenSinkEntity planningEntityProperty,
            List<TestDataKitchenSinkEntity> planningEntityListProperty, String problemFactProperty,
            List<String> problemFactListProperty, HardSoftScore score) {
        this.planningEntityProperty = planningEntityProperty;
        this.planningEntityListProperty = planningEntityListProperty;
        this.problemFactProperty = problemFactProperty;
        this.problemFactListProperty = problemFactListProperty;
        this.score = score;
    }

    public TestDataKitchenSinkEntity getPlanningEntityProperty() {
        return planningEntityProperty;
    }

    public HardSoftScore getScore() {
        return score;
    }
}
