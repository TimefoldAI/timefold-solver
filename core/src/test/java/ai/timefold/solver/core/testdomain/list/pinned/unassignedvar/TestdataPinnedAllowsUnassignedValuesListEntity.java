package ai.timefold.solver.core.testdomain.list.pinned.unassignedvar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataPinnedAllowsUnassignedValuesListEntity extends TestdataObject {

    private List<TestdataPinnedAllowsUnassignedValuesListValue> valueList;

    @PlanningPin
    private boolean pinned;

    public TestdataPinnedAllowsUnassignedValuesListEntity() {
    }

    public TestdataPinnedAllowsUnassignedValuesListEntity(String code) {
        super(code);
        this.valueList = new ArrayList<>();
    }

    public TestdataPinnedAllowsUnassignedValuesListEntity(String code,
            List<TestdataPinnedAllowsUnassignedValuesListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataPinnedAllowsUnassignedValuesListEntity(String code,
            TestdataPinnedAllowsUnassignedValuesListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(allowsUnassignedValues = true, valueRangeProviderRefs = "valueRange")
    public List<TestdataPinnedAllowsUnassignedValuesListValue> getValueList() {
        if (pinned) {
            return Collections.unmodifiableList(valueList);
        }
        return valueList;
    }

    public void setValueList(List<TestdataPinnedAllowsUnassignedValuesListValue> valueList) {
        this.valueList = valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
