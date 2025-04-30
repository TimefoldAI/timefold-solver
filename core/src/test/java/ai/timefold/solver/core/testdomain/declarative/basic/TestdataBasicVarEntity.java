package ai.timefold.solver.core.testdomain.declarative.basic;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataBasicVarEntity {
    String id;
    @PlanningVariable
    TestdataBasicVarValue value;

    public TestdataBasicVarEntity(String id, TestdataBasicVarValue value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataBasicVarValue getValue() {
        return value;
    }

    public void setValue(TestdataBasicVarValue value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TestdataBasicVarEntity{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
