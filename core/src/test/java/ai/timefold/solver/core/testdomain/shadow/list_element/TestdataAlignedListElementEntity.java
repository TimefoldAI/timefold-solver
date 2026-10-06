package ai.timefold.solver.core.testdomain.shadow.list_element;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataAlignedListElementEntity extends TestdataObject {

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataAlignedListElementValue> values = new ArrayList<>();

    int startTime;

    @ShadowVariable(supplierName = "lastEndTimeSupplier")
    Integer lastEndTime;

    public TestdataAlignedListElementEntity() {
    }

    public TestdataAlignedListElementEntity(String code) {
        super(code);
    }

    @ShadowSources("values[].endTime")
    public Integer lastEndTimeSupplier() {
        return values.isEmpty() ? startTime : values.getLast().getEndTime();
    }

    public List<TestdataAlignedListElementValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataAlignedListElementValue> values) {
        this.values = values;
    }

    public int getStartTime() {
        return startTime;
    }

    public Integer getLastEndTime() {
        return lastEndTime;
    }

    public void setLastEndTime(Integer lastEndTime) {
        this.lastEndTime = lastEndTime;
    }
}
