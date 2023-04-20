package ai.timefold.solver.core.impl.testdata.domain.shadow.manytomany;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataManyToManyShadowedEntityUniqueEvents extends TestdataManyToManyShadowedEntity {

    public static EntityDescriptor<TestdataManyToManyShadowedSolution> buildEntityDescriptor() {
        return TestdataManyToManyShadowedSolution.buildSolutionDescriptorRequiresUniqueEvents()
                .findEntityDescriptorOrFail(TestdataManyToManyShadowedEntityUniqueEvents.class);
    }

    private final List<String> composedCodeLog = new ArrayList<>();

    public TestdataManyToManyShadowedEntityUniqueEvents(String code, TestdataValue primaryValue, TestdataValue secondaryValue) {
        super(code, primaryValue, secondaryValue);
    }

    @Override
    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getPrimaryValue() {
        return super.getPrimaryValue();
    }

    @Override
    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getSecondaryValue() {
        return super.getSecondaryValue();
    }

    @Override
    @ShadowVariable(variableListenerClass = ComposedValuesUpdatingVariableListener.class, sourceVariableName = "primaryValue")
    @ShadowVariable(variableListenerClass = ComposedValuesUpdatingVariableListener.class, sourceVariableName = "secondaryValue")
    public String getComposedCode() {
        return super.getComposedCode();
    }

    @Override
    public void setComposedCode(String composedCode) {
        // (2) log composedCode updates for later verification.
        composedCodeLog.add(composedCode);
        super.setComposedCode(composedCode);
    }

    public List<String> getComposedCodeLog() {
        return composedCodeLog;
    }

    @Override
    @PiggybackShadowVariable(shadowVariableName = "composedCode")
    public String getReverseComposedCode() {
        return super.getReverseComposedCode();
    }

    public static class ComposedValuesUpdatingVariableListener
            extends TestdataManyToManyShadowedEntity.ComposedValuesUpdatingVariableListener {

        @Override
        public boolean requiresUniqueEntityEvents() {
            // (1) Override the original listener and require unique entity events.
            return true;
        }
    }
}
