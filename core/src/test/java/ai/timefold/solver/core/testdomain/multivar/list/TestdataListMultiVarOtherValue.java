package ai.timefold.solver.core.testdomain.multivar.list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiVarOtherValue extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "basicValue")
    private List<TestdataListMultiVarEntity> entityList;

    public TestdataListMultiVarOtherValue() {
        // Required for cloner
    }

    public TestdataListMultiVarOtherValue(String code) {
        super(code);
        entityList = new ArrayList<>();
    }

    public List<TestdataListMultiVarEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListMultiVarEntity> entityList) {
        this.entityList = entityList;
    }
}
