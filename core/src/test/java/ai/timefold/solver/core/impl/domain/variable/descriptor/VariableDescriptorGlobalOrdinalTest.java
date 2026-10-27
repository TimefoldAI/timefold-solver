package ai.timefold.solver.core.impl.domain.variable.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarBaseEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListSolution;

import org.junit.jupiter.api.Test;

class VariableDescriptorGlobalOrdinalTest {

    private static void assertDenseAndUnique(SolutionDescriptor<?> solutionDescriptor) {
        var globalOrdinals = solutionDescriptor.getEntityDescriptors().stream()
                .flatMap(entityDescriptor -> entityDescriptor.getDeclaredVariableDescriptors().stream())
                .mapToInt(VariableDescriptor::getGlobalOrdinal)
                .sorted()
                .toArray();
        assertThat(globalOrdinals)
                .containsExactly(IntStream.range(0, solutionDescriptor.getVariableDescriptorCount()).toArray());
    }

    @Test
    void multipleEntityClasses() {
        // Both the entity and the value class declare variables.
        var solutionDescriptor = TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getVariableDescriptorCount()).isGreaterThan(1);
        assertDenseAndUnique(solutionDescriptor);
    }

    @Test
    void inheritedVariableKeepsTheParentOrdinal() {
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(TestdataAddVarSolution.class,
                TestdataAddVarBaseEntity.class, TestdataAddVarChildEntity.class);
        assertDenseAndUnique(solutionDescriptor);
        assertThat(solutionDescriptor.getVariableDescriptorCount()).isEqualTo(2);

        var baseValue = solutionDescriptor.getEntityDescriptorStrict(TestdataAddVarBaseEntity.class)
                .getVariableDescriptor("value");
        var childEntityDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataAddVarChildEntity.class);
        var childValue = childEntityDescriptor.getVariableDescriptor("value");
        var childValue2 = childEntityDescriptor.getVariableDescriptor("value2");
        assertThat(childValue).isSameAs(baseValue);
        // Both have local ordinal 0, but different global ordinals.
        assertThat(childValue.getOrdinal()).isEqualTo(childValue2.getOrdinal());
        assertThat(childValue.getGlobalOrdinal()).isNotEqualTo(childValue2.getGlobalOrdinal());
    }

}
