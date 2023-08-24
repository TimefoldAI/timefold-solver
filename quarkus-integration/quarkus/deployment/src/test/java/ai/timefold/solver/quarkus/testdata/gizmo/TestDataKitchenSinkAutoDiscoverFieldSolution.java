package ai.timefold.solver.quarkus.testdata.gizmo;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.FIELD)
public class TestDataKitchenSinkAutoDiscoverFieldSolution {

    private TestDataKitchenSinkEntity planningEntityProperty;
    private List<TestDataKitchenSinkEntity> planningEntityListProperty;
    private String problemFactProperty;
    private List<String> problemFactListProperty;
    private HardSoftLongScore score;

    public TestDataKitchenSinkAutoDiscoverFieldSolution() {

    }

    public TestDataKitchenSinkAutoDiscoverFieldSolution(TestDataKitchenSinkEntity planningEntityProperty,
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

    public HardSoftLongScore getScore() {
        return score;
    }
}
