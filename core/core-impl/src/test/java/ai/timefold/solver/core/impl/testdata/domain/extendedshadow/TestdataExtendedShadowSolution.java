package ai.timefold.solver.core.impl.testdata.domain.extendedshadow;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataExtendedShadowSolution {
    @PlanningEntityCollectionProperty
    public List<TestdataExtendedShadowShadowEntity> shadowEntityList;

    @ValueRangeProvider
    @ProblemFactCollectionProperty
    public List<TestdataExtendedShadowVariable> planningVariableList;

    // Exists so Quarkus does not return the original solution because there are no planning variables
    @PlanningEntityProperty
    public TestdataEntity testdataEntity;

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> testdataValueList;

    @PlanningScore
    public SimpleScore score;

    public TestdataExtendedShadowSolution() {
    }

    public TestdataExtendedShadowSolution(TestdataExtendedShadowShadowEntity shadowShadowEntity) {
        this.testdataEntity = new TestdataEntity("Entity 1");
        this.testdataValueList = List.of(new TestdataValue("Value 1"));
        this.shadowEntityList = Collections.singletonList(shadowShadowEntity);
        this.planningVariableList = Collections.emptyList();
    }

}
