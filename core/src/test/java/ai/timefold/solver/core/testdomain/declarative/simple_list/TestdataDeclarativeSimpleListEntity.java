package ai.timefold.solver.core.testdomain.declarative.simple_list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeSimpleListEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataDeclarativeSimpleListValue> values;

    int position;
    int startTime;

    public TestdataDeclarativeSimpleListEntity() {
        this.values = new ArrayList<>();
    }

    public TestdataDeclarativeSimpleListEntity(String code, int position, int startTime) {
        super(code);
        this.values = new ArrayList<>();
        this.position = position;
        this.startTime = startTime;
    }

    public List<TestdataDeclarativeSimpleListValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeSimpleListValue> values) {
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
