package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.TestdataEntityProvidingWithParameterEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.TestdataEntityProvidingWithParameterSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.method.TestdataInvalidMethodEntityProvidingWithParameterEntity;

import org.junit.jupiter.api.Test;

class ReflectionExtendedBeanPropertyMemberAccessorTest {

    @Test
    void methodAnnotatedEntity() throws NoSuchMethodException {
        ReflectionExtendedBeanPropertyMemberAccessor memberAccessor = new ReflectionExtendedBeanPropertyMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class));
        assertThat(memberAccessor.getName()).isEqualTo("valueRange");
        assertThat(memberAccessor.getType()).isEqualTo(List.class);
        assertThat(memberAccessor.getAnnotation(ValueRangeProvider.class)).isNotNull();
        assertThat(memberAccessor.getGetterMethodParameterType()).isEqualTo(TestdataEntityProvidingWithParameterSolution.class);

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntityProvidingWithParameterEntity("e1", List.of(v1, v2), v1);
        var s1 = new TestdataEntityProvidingWithParameterSolution("s1");

        assertThat(memberAccessor.executeGetter(e1, s1)).isEqualTo(List.of(v1, v2));
        memberAccessor.executeSetter(e1, List.of(v2));
        assertThat(e1.getValueRange(s1)).isEqualTo(List.of(v2));
    }

    @Test
    void getterSetterVisibilityDoesNotMatch() {
        assertThatCode(() -> new ReflectionExtendedBeanPropertyMemberAccessor(
                TestdataInvalidMethodEntityProvidingWithParameterEntity.class.getDeclaredMethod("getValueRange1",
                        TestdataEntityProvidingWithParameterSolution.class)))
                .hasMessageContainingAll("getterMethod (getValueRange1)",
                        "has access modifier (public)",
                        "not match the setterMethod (setValueRange1)",
                        "access modifier (private)",
                        "on class (ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.method.TestdataInvalidMethodEntityProvidingWithParameterEntity");
        assertThatCode(() -> new ReflectionExtendedBeanPropertyMemberAccessor(
                TestdataInvalidMethodEntityProvidingWithParameterEntity.class.getDeclaredMethod("getValueRange2",
                        TestdataEntityProvidingWithParameterSolution.class)))
                .hasMessageContainingAll("getterMethod (getValueRange2)",
                        "has access modifier (package-private)",
                        "not match the setterMethod (setValueRange2)",
                        "access modifier (protected)",
                        "on class (ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.method.TestdataInvalidMethodEntityProvidingWithParameterEntity)");
    }

    @Test
    void setterMissing() {
        assertThatCode(() -> new ReflectionExtendedBeanPropertyMemberAccessor(
                TestdataInvalidMethodEntityProvidingWithParameterEntity.class.getDeclaredMethod("getValueRangeWithoutSetter")))
                .hasMessageContainingAll("getterMethod (getValueRangeWithoutSetter)",
                        "does not have a matching setterMethod",
                        "on class (ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.method.TestdataInvalidMethodEntityProvidingWithParameterEntity)");
    }

    @Test
    void forbiddenGetterWithoutParameter() {
        assertThatCode(() -> new ReflectionExtendedBeanPropertyMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class),
                true).executeGetter(new TestdataEntityProvidingWithParameterEntity()))
                .hasMessageContainingAll(
                        "The method executeGetter(Object) without parameter is not supported. Maybe call executeGetter(Object, Object) from ExtendedMemberAccessor instead.");
        assertThatCode(() -> new ReflectionExtendedBeanPropertyMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class),
                true).getGetterFunction())
                .hasMessageContainingAll(
                        "The method getGetterFunction() is not supported.");
    }

}
