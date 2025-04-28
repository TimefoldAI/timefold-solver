package ai.timefold.solver.core.testdomain.shadow.multiplelistener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultipleShadowVariableValue extends TestdataObject {

    public static EntityDescriptor<TestdataListMultipleShadowVariableSolution> buildEntityDescriptor() {
        return TestdataListMultipleShadowVariableSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListMultipleShadowVariableValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListMultipleShadowVariableEntity entity;
    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataListMultipleShadowVariableValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataListMultipleShadowVariableValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @ShadowVariable(variableListenerClass = TestdataListMultipleShadowVariableListener.class, sourceVariableName = "entity")
    private Integer listenerValue;
    private final List<TestdataListMultipleShadowVariableEntity> entityHistory = new ArrayList<>();
    private final List<Integer> indexHistory = new ArrayList<>();
    private final List<TestdataListMultipleShadowVariableValue> previousHistory = new ArrayList<>();
    private final List<TestdataListMultipleShadowVariableValue> nextHistory = new ArrayList<>();

    public TestdataListMultipleShadowVariableValue() {
    }

    public TestdataListMultipleShadowVariableValue(String code) {
        super(code);
    }

    public TestdataListMultipleShadowVariableEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListMultipleShadowVariableEntity entity) {
        this.entity = entity;
        entityHistory.add(entity);
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
        indexHistory.add(index);
    }

    public TestdataListMultipleShadowVariableValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataListMultipleShadowVariableValue previous) {
        this.previous = previous;
        previousHistory.add(previous);
    }

    public TestdataListMultipleShadowVariableValue getNext() {
        return next;
    }

    public void setNext(TestdataListMultipleShadowVariableValue next) {
        this.next = next;
        nextHistory.add(next);
    }

    public Integer getCascadeValue() {
        if (cascadeValue == null) {
            return 2;
        }
        return cascadeValue;
    }

    public void updateCascadeValue() {
        this.cascadeValue = index + 10;
    }

    public Integer getListenerValue() {
        if (listenerValue == null) {
            return 0;
        }
        return listenerValue;
    }

    public void setListenerValue(Integer listenerValue) {
        this.listenerValue = listenerValue;
    }

    public List<TestdataListMultipleShadowVariableEntity> getEntityHistory() {
        return Collections.unmodifiableList(entityHistory);
    }

    public List<Integer> getIndexHistory() {
        return Collections.unmodifiableList(indexHistory);
    }

    public List<TestdataListMultipleShadowVariableValue> getPreviousHistory() {
        return Collections.unmodifiableList(previousHistory);
    }

    public List<TestdataListMultipleShadowVariableValue> getNextHistory() {
        return Collections.unmodifiableList(nextHistory);
    }

}
