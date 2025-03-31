package ai.timefold.solver.test.api.score.stream.testdata.shadow.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataListShadowConstraintVerifierEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListShadowConstraintVerifierSolution> buildEntityDescriptor() {
        return TestdataListShadowConstraintVerifierSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListShadowConstraintVerifierEntity.class);
    }

    public static ListVariableDescriptor<TestdataListShadowConstraintVerifierSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListShadowConstraintVerifierSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataListShadowConstraintVerifierEntity createWithValues(String code,
            TestdataListShadowConstraintVerifierValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataListShadowConstraintVerifierEntity(code, values).setUpShadowVariables();
    }

    TestdataListShadowConstraintVerifierEntity setUpShadowVariables() {
        for (int i = 0; i < valueList.size(); i++) {
            TestdataListShadowConstraintVerifierValue value = valueList.get(i);
            value.setEntity(this);
            value.setIndex(i);
            if (i > 0) {
                value.setPrevious(valueList.get(i - 1));
            }
            if (i < valueList.size() - 1) {
                value.setNext(valueList.get(i + 1));
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListShadowConstraintVerifierValue> valueList;

    public TestdataListShadowConstraintVerifierEntity() {
    }

    public TestdataListShadowConstraintVerifierEntity(String code, List<TestdataListShadowConstraintVerifierValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataListShadowConstraintVerifierEntity(String code, TestdataListShadowConstraintVerifierValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public void setValueList(List<TestdataListShadowConstraintVerifierValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListShadowConstraintVerifierValue> getValueList() {
        return valueList;
    }
}
