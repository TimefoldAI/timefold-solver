package ai.timefold.solver.core.testdomain.shadow.full;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataShadowedFullValue extends TestdataObject {
    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    TestdataShadowedFullEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    TestdataShadowedFullValue previousValue;

    @NextElementShadowVariable(sourceVariableName = "valueList")
    TestdataShadowedFullValue nextValue;

    @IndexShadowVariable(sourceVariableName = "valueList")
    Integer index;

    @ShadowVariable(variableListenerClass = TestdataShadowedFullConsistencyListener.class,
            sourceVariableName = "entity")
    @ShadowVariable(variableListenerClass = TestdataShadowedFullConsistencyListener.class,
            sourceVariableName = "previousValue")
    @ShadowVariable(variableListenerClass = TestdataShadowedFullConsistencyListener.class,
            sourceVariableName = "nextValue")
    @ShadowVariable(variableListenerClass = TestdataShadowedFullConsistencyListener.class,
            sourceVariableName = "index")
    Boolean isConsistent;

    public TestdataShadowedFullValue() {

    }

    public TestdataShadowedFullValue(String code) {
        super(code);
    }

    public void updateShadows(TestdataShadowedFullEntity entity, int index) {
        this.index = index;
        this.entity = entity;
        if (index > 0) {
            this.previousValue = entity.getValueList().get(index - 1);
        } else {
            this.previousValue = null;
        }

        if (index < entity.getValueList().size() - 1) {
            this.nextValue = entity.getValueList().get(index + 1);
        } else {
            this.nextValue = null;
        }
    }
}
