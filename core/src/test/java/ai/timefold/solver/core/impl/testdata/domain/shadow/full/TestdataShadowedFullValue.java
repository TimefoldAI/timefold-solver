package ai.timefold.solver.core.impl.testdata.domain.shadow.full;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

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
}
