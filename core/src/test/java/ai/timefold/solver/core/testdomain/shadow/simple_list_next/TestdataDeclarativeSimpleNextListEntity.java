package ai.timefold.solver.core.testdomain.shadow.simple_list_next;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeSimpleNextListEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataDeclarativeSimpleNextListValue> values;

    int position;
    int startTime;

    public TestdataDeclarativeSimpleNextListEntity() {
        this.values = new ArrayList<>();
    }

    public TestdataDeclarativeSimpleNextListEntity(String code, int position, int startTime) {
        super(code);
        this.values = new ArrayList<>();
        this.position = position;
        this.startTime = startTime;
    }

    public List<TestdataDeclarativeSimpleNextListValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeSimpleNextListValue> values) {
        this.values = values;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }
}
