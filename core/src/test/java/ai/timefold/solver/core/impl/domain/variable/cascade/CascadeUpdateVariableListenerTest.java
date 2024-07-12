package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingInverseValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingNextValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingPreviousValue;

import org.junit.jupiter.api.Test;

class CascadeUpdateVariableListenerTest {

    @Test
    void requiredShadowVariableDependencies() {
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeMissingInverseValue::buildEntityDescriptor)
                .withMessageContaining("The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingInverseValue)")
                .withMessageContaining("has an @CascadeUpdateElementShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but has no @InverseRelationShadowVariable shadow variable defined.");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeMissingPreviousValue::buildEntityDescriptor)
                .withMessageContaining("The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingPreviousValue)")
                .withMessageContaining("has an @CascadeUpdateElementShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but has no @PreviousElementShadowVariable shadow variable defined");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeMissingNextValue::buildEntityDescriptor)
                .withMessageContaining("The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingNextValue)")
                .withMessageContaining("has an @CascadeUpdateElementShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but has no @NextElementShadowVariable shadow variable defined");
    }
}
