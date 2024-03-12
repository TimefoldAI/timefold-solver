package ai.timefold.solver.core.impl.testdata.domain.list.externalized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataListEntityExternalized extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListValueExternalized> valueList;

    public TestdataListEntityExternalized() {
    }

    public TestdataListEntityExternalized(String code, List<TestdataListValueExternalized> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataListEntityExternalized(String code, TestdataListValueExternalized... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public List<TestdataListValueExternalized> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListValueExternalized> valueList) {
        this.valueList = valueList;
    }
}
