package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedOtherValue extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "basicValue")
    private List<TestdataMixedEntity> entityList;

    private int strength;

    public TestdataMixedOtherValue() {
        // Required for cloner
    }

    public TestdataMixedOtherValue(String code, int strength) {
        super(code);
        this.strength = strength;
        entityList = new ArrayList<>();
    }

    public List<TestdataMixedEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMixedEntity> entityList) {
        this.entityList = entityList;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }
}
