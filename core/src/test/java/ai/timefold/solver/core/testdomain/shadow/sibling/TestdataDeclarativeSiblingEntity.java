package ai.timefold.solver.core.testdomain.shadow.sibling;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeSiblingEntity extends TestdataObject {
    @PlanningListVariable
    List<AbstractTestdataDeclarativeSiblingValue> values;

    public TestdataDeclarativeSiblingEntity() {
        super();
        this.values = new ArrayList<>();
    }

    public TestdataDeclarativeSiblingEntity(String code) {
        super(code);
        this.values = new ArrayList<>();
    }

    public List<AbstractTestdataDeclarativeSiblingValue> getValues() {
        return values;
    }

    public void setValues(List<AbstractTestdataDeclarativeSiblingValue> values) {
        this.values = values;
    }
}
