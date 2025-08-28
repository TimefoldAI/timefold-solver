package ai.timefold.solver.quarkus.testdomain.suppliervariable.list;

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

    @Override
    public String toString() {
        return name + " " + values;
    }
}
