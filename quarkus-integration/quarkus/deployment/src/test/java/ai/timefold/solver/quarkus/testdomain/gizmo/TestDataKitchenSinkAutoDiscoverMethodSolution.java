package ai.timefold.solver.quarkus.testdomain.gizmo;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.HardSoftScore;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.GETTER)
public class TestDataKitchenSinkAutoDiscoverMethodSolution {

    private TestDataKitchenSinkEntity planningEntityProperty;
    private List<TestDataKitchenSinkEntity> planningEntityListProperty;
    private String problemFactProperty;
    private List<String> problemFactListProperty;
    private HardSoftScore score;

    public TestDataKitchenSinkAutoDiscoverMethodSolution() {

    }

    public TestDataKitchenSinkAutoDiscoverMethodSolution(TestDataKitchenSinkEntity planningEntityProperty,
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

    public void setPlanningEntityProperty(TestDataKitchenSinkEntity planningEntityProperty) {
        this.planningEntityProperty = planningEntityProperty;
    }

    public List<TestDataKitchenSinkEntity> getPlanningEntityListProperty() {
        return planningEntityListProperty;
    }

    public void setPlanningEntityListProperty(List<TestDataKitchenSinkEntity> planningEntityListProperty) {
        this.planningEntityListProperty = planningEntityListProperty;
    }

    public String getProblemFactProperty() {
        return problemFactProperty;
    }

    public void setProblemFactProperty(String problemFactProperty) {
        this.problemFactProperty = problemFactProperty;
    }

    public List<String> getProblemFactListProperty() {
        return problemFactListProperty;
    }

    public void setProblemFactListProperty(List<String> problemFactListProperty) {
        this.problemFactListProperty = problemFactListProperty;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
