package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.multiple;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;

@PlanningSolution
public class TestdataMultipleInheritanceBaseSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    private List<String> valueList;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }
}
