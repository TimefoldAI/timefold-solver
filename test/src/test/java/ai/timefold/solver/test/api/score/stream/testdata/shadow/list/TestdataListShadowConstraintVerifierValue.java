package ai.timefold.solver.test.api.score.stream.testdata.shadow.list;

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
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataListShadowConstraintVerifierValue extends TestdataObject {

    public static EntityDescriptor<TestdataListShadowConstraintVerifierSolution> buildEntityDescriptor() {
        return TestdataListShadowConstraintVerifierSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListShadowConstraintVerifierValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListShadowConstraintVerifierEntity entity;
    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataListShadowConstraintVerifierValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataListShadowConstraintVerifierValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @ShadowVariable(variableListenerClass = TestdataListShadowVariableListener.class, sourceVariableName = "entity")
    private Integer listenerValue;
    private final List<TestdataListShadowConstraintVerifierEntity> entityHistory = new ArrayList<>();
    private final List<Integer> indexHistory = new ArrayList<>();
    private final List<TestdataListShadowConstraintVerifierValue> previousHistory = new ArrayList<>();
    private final List<TestdataListShadowConstraintVerifierValue> nextHistory = new ArrayList<>();

    public TestdataListShadowConstraintVerifierValue() {
    }

    public TestdataListShadowConstraintVerifierValue(String code) {
        super(code);
    }

    public TestdataListShadowConstraintVerifierEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListShadowConstraintVerifierEntity entity) {
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

    public TestdataListShadowConstraintVerifierValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataListShadowConstraintVerifierValue previous) {
        this.previous = previous;
        previousHistory.add(previous);
    }

    public TestdataListShadowConstraintVerifierValue getNext() {
        return next;
    }

    public void setNext(TestdataListShadowConstraintVerifierValue next) {
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
        return listenerValue;
    }

    public void setListenerValue(Integer listenerValue) {
        this.listenerValue = listenerValue;
    }

    public List<TestdataListShadowConstraintVerifierEntity> getEntityHistory() {
        return Collections.unmodifiableList(entityHistory);
    }

    public List<Integer> getIndexHistory() {
        return Collections.unmodifiableList(indexHistory);
    }

    public List<TestdataListShadowConstraintVerifierValue> getPreviousHistory() {
        return Collections.unmodifiableList(previousHistory);
    }

    public List<TestdataListShadowConstraintVerifierValue> getNextHistory() {
        return Collections.unmodifiableList(nextHistory);
    }

}
