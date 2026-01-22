package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeclarativeShadowVariableDescriptorTest {
    @Test
    void groupKeyMemberForNoKey() {
        var member = DeclarativeShadowVariableDescriptor.getAlignmentKeyMemberForEntityProperty(
                null, null, null, "shadow", null);

        assertThat(member).isNull();
    }

    @Test
    @SuppressWarnings({ "rawtype", "unchecked" })
    void groupKeyMemberForEntityPropertyForFact() {
        record Example(int fact) {
        }

        var solutionMetamodel = Mockito.mock(PlanningSolutionMetaModel.class);
        var exampleMetamodel = Mockito.mock(GenuineEntityMetaModel.class);
        var calculator = Mockito.mock(MemberAccessor.class);

        when(solutionMetamodel.entity(Example.class))
                .thenReturn(exampleMetamodel);
        when(solutionMetamodel.hasEntity(Example.class))
                .thenReturn(true);
        when(exampleMetamodel.hasVariable("fact")).thenReturn(false);

        var member = DeclarativeShadowVariableDescriptor.getAlignmentKeyMemberForEntityProperty(
                solutionMetamodel, Example.class, calculator, "shadow", "fact");

        assertThat(member.getName()).isEqualTo("fact");
        assertThat(member.getDeclaringClass()).isEqualTo(Example.class);
    }

    // Must be declared outside of method so it has a canonical name
    private record BadExample(int variable) {
    }

    @Test
    @SuppressWarnings({ "rawtype", "unchecked" })
    void groupKeyMemberForEntityPropertyForVariable() {
        var solutionMetamodel = Mockito.mock(PlanningSolutionMetaModel.class);
        var exampleMetamodel = Mockito.mock(GenuineEntityMetaModel.class);
        var calculator = Mockito.mock(MemberAccessor.class);

        when(solutionMetamodel.entity(BadExample.class))
                .thenReturn(exampleMetamodel);
        when(solutionMetamodel.hasEntity(BadExample.class))
                .thenReturn(true);
        when(exampleMetamodel.hasVariable("variable")).thenReturn(true);
        when(calculator.getName()).thenReturn("valueSupplier");

        assertThatCode(() -> DeclarativeShadowVariableDescriptor.getAlignmentKeyMemberForEntityProperty(
                solutionMetamodel, BadExample.class, calculator, "shadow", "variable"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(
                        "@" + ShadowSources.class.getSimpleName(),
                        "annotated supplier method (valueSupplier)",
                        "for variable (shadow)",
                        "on class (" + BadExample.class.getCanonicalName() + ")",
                        "uses a alignmentKey (variable) that is a variable");
    }
}
