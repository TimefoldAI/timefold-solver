package ai.timefold.solver.core.testdomain.planningid;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;

@PlanningSolution
public class TestdataStringPlanningIdSolution extends TestdataSolution {
    private List<String> stringValueList;
    private List<TestdataStringPlanningIdEntity> stringEntityList;

    @ValueRangeProvider(id = "stringValueRange")
    @ProblemFactCollectionProperty
    public List<String> getStringValueList() {
        return stringValueList;
    }

    public void setStringValueList(List<String> stringValueList) {
        this.stringValueList = stringValueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataStringPlanningIdEntity> getStringEntityList() {
        return stringEntityList;
    }

    public void setStringEntityList(List<TestdataStringPlanningIdEntity> stringEntityList) {
        this.stringEntityList = stringEntityList;
    }
}
