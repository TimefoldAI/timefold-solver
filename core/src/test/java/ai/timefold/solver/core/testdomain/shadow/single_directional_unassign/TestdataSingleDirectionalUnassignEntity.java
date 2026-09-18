package ai.timefold.solver.core.testdomain.shadow.single_directional_unassign;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataSingleDirectionalUnassignEntity extends TestdataObject {

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataSingleDirectionalUnassignValue> values;

    public TestdataSingleDirectionalUnassignEntity() {
        values = new ArrayList<>();
    }

    public TestdataSingleDirectionalUnassignEntity(String code) {
        super(code);
        values = new ArrayList<>();
    }

    public List<TestdataSingleDirectionalUnassignValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataSingleDirectionalUnassignValue> values) {
        this.values = values;
    }

}
