package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.TestdataEntityProvidingWithParameterEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.TestdataEntityProvidingWithParameterSolution;

import org.junit.jupiter.api.Test;

class ReflectionMethodExtendedMemberAccessorTest {

    @Test
    void methodAnnotatedEntity() throws NoSuchMethodException {
        var memberAccessor = new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class));
        assertThat(memberAccessor.getName()).isEqualTo("getValueRange");
        assertThat(memberAccessor.getType()).isEqualTo(List.class);
        assertThat(memberAccessor.getAnnotation(ValueRangeProvider.class)).isNotNull();
        assertThat(memberAccessor.getGetterMethodParameterType()).isEqualTo(TestdataEntityProvidingWithParameterSolution.class);

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntityProvidingWithParameterEntity("e1", List.of(v1, v2), v1);
        var s1 = new TestdataEntityProvidingWithParameterSolution("s1");

        assertThat(memberAccessor.executeGetter(e1, s1)).isEqualTo(List.of(v1, v2));
        e1.setValueRange(List.of(v2));
        assertThat(e1.getValueRange(s1)).isEqualTo(List.of(v2));
    }

    @Test
    void forbiddenReadWithoutParameter() {
        assertThatCode(() -> new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class),
                true).executeGetter(new TestdataEntityProvidingWithParameterEntity()))
                .hasMessageContainingAll(
                        "The method executeGetter(Object) without parameter is not supported. Maybe call executeGetter(Object, Object) from ExtendedMemberAccessor instead.");
        assertThatCode(() -> new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class),
                true).getGetterFunction())
                .hasMessageContainingAll(
                        "The method getGetterFunction() is not supported.");
    }

}
