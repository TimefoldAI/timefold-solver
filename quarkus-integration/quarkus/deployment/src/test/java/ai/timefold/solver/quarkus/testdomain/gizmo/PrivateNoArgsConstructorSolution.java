package ai.timefold.solver.quarkus.testdomain.gizmo;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class PrivateNoArgsConstructorSolution {
    @PlanningEntityCollectionProperty
    List<PrivateNoArgsConstructorEntity> planningEntityList;

    @PlanningScore
    public SimpleScore score;

    public final int someField;

    private PrivateNoArgsConstructorSolution() {
        this.someField = 1;
    }

    public PrivateNoArgsConstructorSolution(List<PrivateNoArgsConstructorEntity> planningEntityList) {
        this.planningEntityList = planningEntityList;
        this.someField = 2;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<String> valueRange() {
        return Arrays.asList("1", "2", "3");
    }

    public List<PrivateNoArgsConstructorEntity> getPlanningEntityList() {
        return planningEntityList;
    }

    public void setPlanningEntityList(
            List<PrivateNoArgsConstructorEntity> planningEntityList) {
        this.planningEntityList = planningEntityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public int getSomeField() {
        return someField;
    }
}
