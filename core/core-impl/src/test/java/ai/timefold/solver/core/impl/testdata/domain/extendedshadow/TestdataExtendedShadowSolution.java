package ai.timefold.solver.core.impl.testdata.domain.extendedshadow;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataExtendedShadowSolution {

    @PlanningEntityCollectionProperty
    public List<TestdataExtendedShadowShadowEntity> shadowEntityList;

    @ValueRangeProvider
    @ProblemFactCollectionProperty
    public List<TestdataExtendedShadowVariable> planningVariableList;

    @PlanningScore
    public SimpleScore score;

    public TestdataExtendedShadowSolution() {
    }

    public TestdataExtendedShadowSolution(TestdataExtendedShadowShadowEntity shadowShadowEntity) {
        this.shadowEntityList = Collections.singletonList(shadowShadowEntity);
        this.planningVariableList = Collections.emptyList();
    }

}
