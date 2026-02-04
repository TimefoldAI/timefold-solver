package ai.timefold.solver.quarkus.testdomain.declarative.list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataQuarkusDeclarativeShadowVariableListEntity {
    String name;

    @PlanningListVariable
    List<TestdataQuarkusDeclarativeShadowVariableListValue> values;

    public TestdataQuarkusDeclarativeShadowVariableListEntity() {
        this.values = new ArrayList<>();
    }

    public TestdataQuarkusDeclarativeShadowVariableListEntity(String name) {
        this.name = name;
        this.values = new ArrayList<>();
    }

    public List<TestdataQuarkusDeclarativeShadowVariableListValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataQuarkusDeclarativeShadowVariableListValue> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return name + " " + values;
    }
}
