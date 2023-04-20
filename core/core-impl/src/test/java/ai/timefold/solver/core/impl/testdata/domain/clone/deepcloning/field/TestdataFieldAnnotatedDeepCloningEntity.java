package ai.timefold.solver.core.impl.testdata.domain.clone.deepcloning.field;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.DummyVariableListener;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataFieldAnnotatedDeepCloningEntity extends TestdataObject {

    public static EntityDescriptor<TestdataFieldAnnotatedDeepCloningSolution> buildEntityDescriptor() {
        return TestdataFieldAnnotatedDeepCloningSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataFieldAnnotatedDeepCloningEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataFieldAnnotatedDeepCloningSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private TestdataValue value;
    @DeepPlanningClone
    @ShadowVariable(variableListenerClass = DummyVariableListener.class, sourceVariableName = "value")
    private List<String> shadowVariableList;
    @DeepPlanningClone
    @ShadowVariable(variableListenerClass = DummyVariableListener.class, sourceVariableName = "value")
    private Map<String, String> shadowVariableMap;

    @DeepPlanningClone
    private Map<List<String>, String> stringListToStringMap = new HashMap<>();

    @DeepPlanningClone
    private Map<String, List<String>> stringToStringListMap = new HashMap<>();

    public TestdataFieldAnnotatedDeepCloningEntity() {
    }

    public TestdataFieldAnnotatedDeepCloningEntity(String code) {
        super(code);
    }

    public TestdataFieldAnnotatedDeepCloningEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    public List<String> getShadowVariableList() {
        return shadowVariableList;
    }

    public void setShadowVariableList(List<String> shadowVariableList) {
        this.shadowVariableList = shadowVariableList;
    }

    public Map<String, String> getShadowVariableMap() {
        return shadowVariableMap;
    }

    public void setShadowVariableMap(Map<String, String> shadowVariableMap) {
        this.shadowVariableMap = shadowVariableMap;
    }

    public Map<List<String>, String> getStringListToStringMap() {
        return stringListToStringMap;
    }

    public void setStringListToStringMap(Map<List<String>, String> stringListToStringMap) {
        this.stringListToStringMap = stringListToStringMap;
    }

    public Map<String, List<String>> getStringToStringListMap() {
        return stringToStringListMap;
    }

    public void setStringToStringListMap(Map<String, List<String>> stringToStringListMap) {
        this.stringToStringListMap = stringToStringListMap;
    }
}
