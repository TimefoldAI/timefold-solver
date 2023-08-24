package ai.timefold.solver.quarkus.testdata.gizmo;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.GETTER)
public class TestDataKitchenSinkAutoDiscoverMethodSolution {

    private TestDataKitchenSinkEntity planningEntityProperty;
    private List<TestDataKitchenSinkEntity> planningEntityListProperty;
    private String problemFactProperty;
    private List<String> problemFactListProperty;
    private HardSoftLongScore score;

    public TestDataKitchenSinkAutoDiscoverMethodSolution() {

    }

    public TestDataKitchenSinkAutoDiscoverMethodSolution(TestDataKitchenSinkEntity planningEntityProperty,
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

    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }
}
