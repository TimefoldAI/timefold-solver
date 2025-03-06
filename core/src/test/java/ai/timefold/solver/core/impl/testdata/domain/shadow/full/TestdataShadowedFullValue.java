package ai.timefold.solver.core.impl.testdata.domain.shadow.full;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
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

    public TestdataShadowedFullValue() {

    }

    public TestdataShadowedFullValue(String code) {
        super(code);
    }

    public TestdataShadowedFullEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataShadowedFullEntity entity) {
        this.entity = entity;
    }

    public TestdataShadowedFullValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataShadowedFullValue previousValue) {
        this.previousValue = previousValue;
    }

    public TestdataShadowedFullValue getNextValue() {
        return nextValue;
    }

    public void setNextValue(TestdataShadowedFullValue nextValue) {
        this.nextValue = nextValue;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
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
