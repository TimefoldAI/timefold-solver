package ai.timefold.solver.quarkus.testdomain.suppliervariable.list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataQuarkusSupplierVariableListEntity {
    String name;

    @PlanningListVariable
    List<TestdataQuarkusSupplierVariableListValue> values;

    public TestdataQuarkusSupplierVariableListEntity() {
        this.values = new ArrayList<>();
    }

    public TestdataQuarkusSupplierVariableListEntity(String name) {
        this.name = name;
        this.values = new ArrayList<>();
    }

    @Override
    public String toString() {
        return name + " " + values;
    }
}
