package ai.timefold.solver.core.testdomain.shadow.list_element;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedListElementEntity extends TestdataObject {

    @PlanningListVariable
    List<TestdataMixedListElementValue> values = new ArrayList<>();

    @ShadowVariable(supplierName = "totalDurationSupplier")
    Integer totalDuration;

    public TestdataMixedListElementEntity() {
    }

    public TestdataMixedListElementEntity(String code) {
        super(code);
    }

    @ShadowSources("values[].paddedDuration")
    public Integer totalDurationSupplier() {
        var total = 0;
        for (var value : values) {
            if (value.getPaddedDuration() == null) {
                return null;
            }
            total += value.getPaddedDuration();
        }
        return total;
    }

    public List<TestdataMixedListElementValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataMixedListElementValue> values) {
        this.values = values;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }
}
